import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
import java.util.Random;

public class HideMsgInPicture {

    final static long HEADSIZE = 120;
    private static double noiseIntensity = 20.0; // Интенсивность шума (можно изменить по вашему усмотрению)

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Steganography Encoder/Decoder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        JButton encodeButton = new JButton("Encode");
        JButton decodeButton = new JButton("Decode");
        JButton increaseNoiseButton = new JButton("Increase Noise");
        JTextField messageField = new JTextField(20);

        JLabel originalImageLabel = new JLabel();
        JLabel encodedImageLabel = new JLabel();
        JLabel changedPixelsLabel = new JLabel();
        JLabel decodedMessageLabel = new JLabel();

        encodeButton.addActionListener(e -> {
            JFileChooser imageChooser = new JFileChooser();
            int result = imageChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File imageFile = imageChooser.getSelectedFile();

                try {
                    String encodedImagePath = encode(imageFile.getAbsolutePath(), messageField.getText());
                    BufferedImage originalImage = ImageIO.read(new File(imageFile.getAbsolutePath()));
                    ImageIcon originalImageIcon = new ImageIcon(originalImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
                    originalImageLabel.setIcon(originalImageIcon);

                    BufferedImage encodedImage = ImageIO.read(new File(encodedImagePath));
                    ImageIcon encodedImageIcon = new ImageIcon(encodedImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
                    encodedImageLabel.setIcon(encodedImageIcon);

                    BufferedImage changedPixelsImage = getChangedPixelsImage(originalImage, encodedImage);
                    ImageIcon changedPixelsIcon = new ImageIcon(changedPixelsImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
                    changedPixelsLabel.setIcon(changedPixelsIcon);

                    saveImage(changedPixelsImage, "changed_pixels_image.bmp");
                    JOptionPane.showMessageDialog(null, "Encoding complete! Encoded image saved in the same directory as the executable JAR file.");

                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error encoding the files.");
                }
            }
        });

        decodeButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(null);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    String decodedMessage = decode(selectedFile.getAbsolutePath());
                    int indexOfSymbol = decodedMessage.indexOf('@');
                    if (indexOfSymbol != -1) {
                        decodedMessage = decodedMessage.substring(0, indexOfSymbol);
                    }
                    decodedMessageLabel.setText("Decoded Message: " + decodedMessage);

                    BufferedImage encodedImage = ImageIO.read(selectedFile);
                    ImageIcon encodedImageIcon = new ImageIcon(encodedImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
                    encodedImageLabel.setIcon(encodedImageIcon);

                    BufferedImage adversarialNoiseImage = applyAdversarialNoise(encodedImage);
                    ImageIcon adversarialNoiseIcon = new ImageIcon(adversarialNoiseImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
                    changedPixelsLabel.setIcon(adversarialNoiseIcon);

                    JOptionPane.showMessageDialog(null, "Decoding complete!\nDecoded Message: " + decodedMessage);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error decoding the file.");
                }
            }
        });

        increaseNoiseButton.addActionListener(e -> {
            // Увеличиваем интенсивность шума при каждом нажатии кнопки
            noiseIntensity += 5.0;
            BufferedImage encodedImage = getEncodedImage();
            BufferedImage adversarialNoiseImage = applyAdversarialNoise(encodedImage);
            ImageIcon adversarialNoiseIcon = new ImageIcon(adversarialNoiseImage.getScaledInstance(200, 200, Image.SCALE_DEFAULT));
            changedPixelsLabel.setIcon(adversarialNoiseIcon);
        });

        panel.add(new JLabel("Enter Message:"));
        panel.add(messageField);
        panel.add(encodeButton);
        panel.add(decodeButton);
        panel.add(increaseNoiseButton);

        JPanel previewPanel = new JPanel();
        previewPanel.add(new JLabel("Original Image:"));
        previewPanel.add(originalImageLabel);
        previewPanel.add(new JLabel("Encoded Image:"));
        previewPanel.add(encodedImageLabel);
        previewPanel.add(new JLabel("Changed Pixels (including Adversarial Noise):"));
        previewPanel.add(changedPixelsLabel);
        previewPanel.add(decodedMessageLabel);

        frame.getContentPane().add(BorderLayout.NORTH, panel);
        frame.getContentPane().add(BorderLayout.CENTER, previewPanel);
        frame.setSize(600, 400);
        frame.setVisible(true);
    }

    private static String encode(String imagePath, String message) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(imagePath);
            out = new FileOutputStream("encoded_image.bmp");
            int c;
            int mb;
            byte clearBit1 = (byte) 0xFE; // 11111110

            for (int i = 1; i <= HEADSIZE; i++) {
                out.write(in.read()); // copy header
            }

            for (int i = 0; i < message.length(); i++) {
                mb = message.charAt(i);

                for (int bit = 7; bit >= 0; bit--) {
                    c = in.read() & clearBit1;  // get picturebyte, clear last bit
                    c = (c | ((mb >> bit) & 1));// put msg-bit in end of pic-byte
                    out.write(c);               // add pic-byte in new file
                }
            }

            for (int bit = 7; bit >= 0; bit--) {
                c = in.read() & clearBit1;  // get picturebyte, clear last bit
                out.write(c);               // add pic-byte in new file
            }

            while ((c = in.read()) != -1) out.write(c); // copy the rest of the file
            return "encoded_image.bmp";
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    private static String decode(String imagePath) throws IOException {
        FileInputStream in = null;
        ByteArrayOutputStream out = null;

        try {
            in = new FileInputStream(imagePath);
            out = new ByteArrayOutputStream();

            for (int i = 1; i <= HEADSIZE; i++) {
                in.read(); // skip header
            }

            int decodedByte = 0;
            int bitCount = 0;

            int clearBit1 = 0xFE; // 11111110

            int c;
            while ((c = in.read()) != -1 && bitCount < 8) {
                int lsb = c & 1; // extract the least significant bit
                decodedByte = (decodedByte << 1) | lsb; // add bit to decoded byte
                bitCount++;

                if (bitCount == 8) {
                    out.write(decodedByte); // write decoded byte to ByteArrayOutputStream
                    decodedByte = 0;
                    bitCount = 0;
                }
            }

            return out.toString();
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    private static BufferedImage getChangedPixelsImage(BufferedImage originalImage, BufferedImage encodedImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        BufferedImage changedPixelsImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int originalRGB = originalImage.getRGB(x, y);
                int encodedRGB = encodedImage.getRGB(x, y);

                if (originalRGB != encodedRGB) {
                    changedPixelsImage.setRGB(x, y, Color.RED.getRGB()); // Измененные пиксели красные
                } else {
                    changedPixelsImage.setRGB(x, y, Color.GREEN.getRGB()); // Неизмененные пиксели зеленые
                }
            }
        }

        return changedPixelsImage;
    }

    private static void saveImage(BufferedImage image, String fileName) {
        try {
            ImageIO.write(image, "bmp", new File(getExecutablePath() + File.separator + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage applyAdversarialNoise(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage noisyImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Random random = new Random();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extracting the RGB components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Adding adversarial noise
                int noise = (int) (noiseIntensity * random.nextGaussian());
                red = clamp(red + noise);
                green = clamp(green + noise);
                blue = clamp(blue + noise);

                // Combining the components
                int noisyRGB = (red << 16) | (green << 8) | blue;
                noisyImage.setRGB(x, y, noisyRGB);
            }
        }

        return noisyImage;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private static BufferedImage getEncodedImage() {
        try {
            return ImageIO.read(new File(getExecutablePath() + File.separator + "encoded_image.bmp"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getExecutablePath() {
        try {
            String path = HideMsgInPicture.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            return new File(path).getParent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
