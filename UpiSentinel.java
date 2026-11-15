import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;

/**
 * UPI Sentinel - single-file educational demo.
 * Uses transparent rule-based scoring on simulated transaction details.
 * It does not connect to banks, UPI, or the original project's ML model/database.
 */
public class UpiSentinel extends JFrame {
    private final JTextField amountField = new JTextField("25000", 12);
    private final JTextField qrTextField = new JTextField("");
    private final JLabel imagePreview = new JLabel("No QR image selected", SwingConstants.CENTER);
    private File selectedQrImage;
    private final JCheckBox newDevice = new JCheckBox("New/unrecognized device");
    private final JCheckBox unusualLocation = new JCheckBox("Unusual location");
    private final JCheckBox rapidTransactions = new JCheckBox("Many transactions in a short time");
    private final JCheckBox newPayee = new JCheckBox("First payment to this receiver");
    private final JCheckBox screenSharing = new JCheckBox("Screen sharing / remote-access app detected");
    private final JTextArea resultArea = new JTextArea(8, 42);
    private final DefaultListModel<String> historyModel = new DefaultListModel<>();
    private final JList<String> historyList = new JList<>(historyModel);
    private int transactionNumber = 0;

    public UpiSentinel() {
        super("UPI Sentinel | Fraud Risk Demo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(new EmptyBorder(18, 18, 18, 18));
        root.setBackground(new Color(245, 247, 251));
        setContentPane(root);

        JLabel title = new JLabel("UPI SENTINEL");
        title.setFont(new Font("SansSerif", Font.BOLD, 26));
        title.setForeground(new Color(27, 46, 83));
        JLabel subtitle = new JLabel("Simple transaction risk checker • Educational simulation only");
        subtitle.setForeground(new Color(90, 100, 120));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 4));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);
        root.add(heading, BorderLayout.NORTH);

        JPanel main = new JPanel(new GridLayout(1, 2, 14, 0));
        main.setOpaque(false);
        root.add(main, BorderLayout.CENTER);

        JPanel form = cardPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(sectionTitle("Analyze a transaction"));
        form.add(Box.createVerticalStrut(12));
        form.add(new JLabel("Transaction amount (₹):"));
        form.add(Box.createVerticalStrut(5));
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(amountField);
        form.add(Box.createVerticalStrut(15));
        form.add(new JLabel("QR image (optional):"));
        imagePreview.setPreferredSize(new Dimension(260, 120));
        imagePreview.setBorder(BorderFactory.createDashedBorder(new Color(160, 170, 190)));
        imagePreview.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(imagePreview);
        JButton chooseImage = new JButton("Choose QR image...");
        chooseImage.setAlignmentX(Component.LEFT_ALIGNMENT);
        chooseImage.addActionListener(e -> chooseQrImage());
        form.add(Box.createVerticalStrut(6));
        form.add(chooseImage);
        form.add(Box.createVerticalStrut(8));
        form.add(new JLabel("QR text / payment link (enter or paste):"));
        qrTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        form.add(qrTextField);
        form.add(Box.createVerticalStrut(12));
        form.add(new JLabel("Select any signals that apply:"));
        form.add(Box.createVerticalStrut(8));
        for (JCheckBox box : new JCheckBox[]{newDevice, unusualLocation, rapidTransactions, newPayee, screenSharing}) {
            box.setOpaque(false);
            box.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(box);
            form.add(Box.createVerticalStrut(7));
        }
        JButton analyze = new JButton("Analyze transaction");
        analyze.setFont(new Font("SansSerif", Font.BOLD, 14));
        analyze.setBackground(new Color(35, 93, 190));
        analyze.setForeground(Color.WHITE);
        analyze.setFocusPainted(false);
        analyze.setAlignmentX(Component.LEFT_ALIGNMENT);
        analyze.addActionListener(e -> analyzeTransaction());
        form.add(Box.createVerticalStrut(12));
        form.add(analyze);
        JButton sample = new JButton("Load a suspicious example");
        sample.setAlignmentX(Component.LEFT_ALIGNMENT);
        sample.addActionListener(e -> {
            amountField.setText("25000");
            newDevice.setSelected(true);
            unusualLocation.setSelected(true);
            rapidTransactions.setSelected(true);
            newPayee.setSelected(true);
            screenSharing.setSelected(false);
            qrTextField.setText("upi://pay?pa=unknown@upi&am=25000&cu=INR");
        });
        form.add(Box.createVerticalStrut(7));
        form.add(sample);
        form.add(Box.createVerticalGlue());

        JPanel right = new JPanel(new BorderLayout(0, 12));
        right.setOpaque(false);
        JPanel resultCard = cardPanel();
        resultCard.setLayout(new BorderLayout(0, 8));
        resultCard.add(sectionTitle("Analysis result"), BorderLayout.NORTH);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultArea.setText("Enter transaction details and click Analyze.\n\nThis demo uses simple rules, not a trained AI model.");
        resultArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        resultCard.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        right.add(resultCard, BorderLayout.CENTER);

        JPanel historyCard = cardPanel();
        historyCard.setLayout(new BorderLayout(0, 8));
        historyCard.add(sectionTitle("Session history"), BorderLayout.NORTH);
        historyList.setVisibleRowCount(5);
        historyCard.add(new JScrollPane(historyList), BorderLayout.CENTER);
        JButton clear = new JButton("Clear history");
        clear.addActionListener(e -> historyModel.clear());
        historyCard.add(clear, BorderLayout.SOUTH);
        right.add(historyCard, BorderLayout.SOUTH);
        main.add(form);
        main.add(right);

        JLabel footer = new JLabel("Demo only — never use this score to approve or block real payments.");
        footer.setForeground(new Color(130, 70, 30));
        root.add(footer, BorderLayout.SOUTH);
    }

    private void chooseQrImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a QR code image");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files (PNG, JPG, JPEG, BMP, GIF)", "png", "jpg", "jpeg", "bmp", "gif"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        selectedQrImage = chooser.getSelectedFile();
        try {
            BufferedImage image = ImageIO.read(selectedQrImage);
            if (image == null) throw new IllegalArgumentException("Unsupported image format");
            Image scaled = image.getScaledInstance(260, 120, Image.SCALE_SMOOTH);
            imagePreview.setIcon(new ImageIcon(scaled));
            imagePreview.setText("");
            imagePreview.setToolTipText(selectedQrImage.getName());
            resultArea.setText("QR image loaded: " + selectedQrImage.getName()
                    + "\n\nImportant: this one-file Java demo can display the image, but it does not decode QR pixels. "
                    + "Paste the QR's decoded text or UPI link into the QR text field, then click Analyze.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not open image: " + ex.getMessage(),
                    "Image error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 239)), new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 17));
        label.setForeground(new Color(35, 48, 75));
        return label;
    }

    private void analyzeTransaction() {
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
            if (!Double.isFinite(amount) || amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid amount (zero or greater).", "Invalid amount", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        String qrText = qrTextField.getText().trim();
        String lowerQr = qrText.toLowerCase(Locale.ROOT);
        if (selectedQrImage != null) reasons.add("QR image attached: " + selectedQrImage.getName() + " (image preview only)");
        if (!qrText.isEmpty()) {
            if (lowerQr.startsWith("upi://")) {
                reasons.add("UPI payment link provided for review");
                if (lowerQr.contains("am=")) {
                    try {
                        String amountParam = lowerQr.split("am=")[1].split("[&\s]")[0];
                        double qrAmount = Double.parseDouble(amountParam);
                        if (Math.abs(qrAmount - amount) > 0.01) {
                            score += 25;
                            reasons.add("QR amount differs from entered transaction amount: +25");
                        }
                    } catch (Exception ignored) { }
                }
            } else if (lowerQr.startsWith("http://")) {
                score += 25;
                reasons.add("QR text uses an unencrypted http:// link: +25");
            } else if (lowerQr.startsWith("https://")) {
                reasons.add("HTTPS link detected; HTTPS alone does not prove a link is safe");
                String host = lowerQr.replaceFirst("^https://", "").split("[/?:#]")[0];
                if (host.matches("[0-9.]+") || host.contains("xn--") || host.matches(".*[0-9]{5,}.*")) {
                    score += 20;
                    reasons.add("Link host has an unusual pattern: +20");
                }
            } else if (lowerQr.contains("upi://") || lowerQr.contains("http://") || lowerQr.contains("https://")) {
                score += 20;
                reasons.add("QR text contains a link in an unexpected format: +20");
            } else {
                reasons.add("QR text supplied; no recognizable UPI or web link prefix");
            }
        }
        if (amount >= 20000) { score += 20; reasons.add("Large amount (₹20,000 or more): +20"); }
        else if (amount >= 10000) { score += 10; reasons.add("Elevated amount (₹10,000 or more): +10"); }
        if (newDevice.isSelected()) { score += 20; reasons.add("Unrecognized device: +20"); }
        if (unusualLocation.isSelected()) { score += 20; reasons.add("Unusual location: +20"); }
        if (rapidTransactions.isSelected()) { score += 20; reasons.add("Rapid repeated transactions: +20"); }
        if (newPayee.isSelected()) { score += 10; reasons.add("First payment to receiver: +10"); }
        if (screenSharing.isSelected()) { score += 30; reasons.add("Screen sharing/remote access: +30"); }
        score = Math.min(score, 100);

        String level;
        if (score >= 60) level = "HIGH RISK — verify carefully";
        else if (score >= 30) level = "MEDIUM RISK — extra checks recommended";
        else level = "LOW RISK — few warning signals";

        StringBuilder out = new StringBuilder();
        out.append("RISK SCORE: ").append(score).append(" / 100\n");
        out.append("CLASSIFICATION: ").append(level).append("\n");
        out.append("AMOUNT: ₹").append(String.format(Locale.US, "%,.2f", amount)).append("\n\n");
        out.append("QR IMAGE: ").append(selectedQrImage == null ? "not selected" : selectedQrImage.getName()).append("\n");
        out.append("WHY THIS SCORE?\n");
        if (reasons.isEmpty()) out.append("• No selected warning signals.\n");
        else for (String reason : reasons) out.append("• ").append(reason).append("\n");
        out.append("\nThis is a transparent demo rule score, not a real fraud probability.");
        if (selectedQrImage != null) out.append("\nNote: image is previewed, not automatically decoded. Paste decoded QR text/link above.");
        resultArea.setText(out.toString());
        resultArea.setCaretPosition(0);
        transactionNumber++;
        historyModel.insertElementAt("#" + transactionNumber + " | ₹" + String.format(Locale.US, "%,.0f", amount) + " | " + score + "/100 | " + level.split(" — ")[0], 0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UpiSentinel().setVisible(true));
    }
}