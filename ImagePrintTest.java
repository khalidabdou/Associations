import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImagePrintTest {

    private static Font getArabicFont(int style, int size) {
        String[] candidates = new String[]{"Arial", "Tahoma", "Segoe UI", "Noto Sans Arabic", "Droid Sans Arabic"};
        for (String family : candidates) {
            try {
                Font font = new Font(family, style, size);
                if (font.canDisplay('ا')) return font;
            } catch (Exception e) {}
        }
        return new Font("Dialog", style, size);
    }

    private static void centerString(Graphics2D g2d, String text, int y, int width) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, y);
    }

    public static void main(String[] args) {
        try {
            Printable printable = new Printable() {
                public int print(Graphics graphics, PageFormat pf, int pageIndex) {
                    if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
                    Graphics2D g2d = (Graphics2D) graphics;
                    g2d.translate(pf.getImageableX(), pf.getImageableY());
                    int w = (int) pf.getImageableWidth();
                    int y = 20;

                    g2d.setFont(getArabicFont(Font.BOLD, 14));
                    g2d.setColor(Color.BLACK);
                    centerString(g2d, "اختبار طباعة", y, w);
                    y += 30;
                    g2d.setStroke(new BasicStroke(1f));
                    g2d.drawLine(10, y, w - 10, y);
                    y += 25;
                    g2d.setFont(getArabicFont(Font.PLAIN, 10));
                    centerString(g2d, "طابعة حرارية POS 80mm", y, w);
                    y += 18;
                    centerString(g2d, "Bluetooth Printer (macOS)", y, w);
                    y += 25;
                    g2d.drawLine(10, y, w - 10, y);
                    y += 25;
                    g2d.setFont(getArabicFont(Font.BOLD, 12));
                    g2d.setColor(new Color(46, 125, 50));
                    centerString(g2d, "✓ تم الاتصال بنجاح", y, w);
                    y += 25;
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(getArabicFont(Font.PLAIN, 9));
                    String now = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm").format(new java.util.Date());
                    centerString(g2d, now, y, w);
                    return Printable.PAGE_EXISTS;
                }
            };

            double widthPt = 80.0 * 72.0 / 25.4;
            double heightPt = 420.0 * 72.0 / 25.4;
            PageFormat pageFormat = new PageFormat();
            Paper paper = new Paper();
            paper.setSize(widthPt, heightPt);
            paper.setImageableArea(8.0, 8.0, widthPt - 16.0, heightPt - 16.0);
            pageFormat.setPaper(paper);
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            int widthPx = 576;
            double scale = widthPx / widthPt;
            int heightPx = (int) (heightPt * scale);

            BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, widthPx, heightPx);
            g2d.scale(scale, scale);

            printable.print(g2d, pageFormat, 0);
            g2d.dispose();

            // Crop
            int cropHeight = image.getHeight();
            for (int y = image.getHeight() - 1; y >= 0; y--) {
                boolean hasContent = false;
                for (int x = 0; x < image.getWidth(); x++) {
                    int rgb = image.getRGB(x, y);
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    if (r < 240 || g < 240 || b < 240) {
                        hasContent = true;
                        break;
                    }
                }
                if (hasContent) {
                    cropHeight = y + 24;
                    break;
                }
            }
            int finalHeight = Math.min(cropHeight, image.getHeight());
            if (finalHeight < 100) finalHeight = 100;

            BufferedImage cropped = image.getSubimage(0, 0, image.getWidth(), finalHeight);
            ImageIO.write(cropped, "png", new File("test_print.png"));
            System.out.println("Image saved successfully to test_print.png, height: " + finalHeight);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
