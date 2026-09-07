import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * Generates the repository images:
 *   assets/icon-512.png  - app icon for the repo
 *   assets/icon-192.png  - smaller app icon
 *   assets/social-preview.png - 1280x640 banner used by GitHub as the social
 *                               preview of the repository
 * Uses the same glyph geometry as brand/logo.svg and IconRenderer.
 */
public class AssetsGen {

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File("app/src/main/ic_launcher-playstore.png"));
        File out = new File("assets");
        Files.createDirectories(out.toPath());
        ImageIO.write(scale(src, 512), "png", new File(out, "icon-512.png"));
        ImageIO.write(scale(src, 192), "png", new File(out, "icon-192.png"));
        ImageIO.write(new AssetsGen().socialPreview(), "png", new File(out, "social-preview.png"));
        System.out.println("wrote assets/icon-512.png, assets/icon-192.png, assets/social-preview.png");
    }

    static BufferedImage scale(BufferedImage src, int n) {
        BufferedImage out = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, n, n, null);
        g.dispose();
        return out;
    }

    BufferedImage icon(int n) {
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        GradientPaint grad = new GradientPaint(0, 0, new Color(0x123126), 0, n, new Color(0x060C09));
        g.setPaint(grad);
        float radius = n * 0.22f;
        g.fill(new RoundRectangle2D.Double(0, 0, n, n, radius, radius));
        g.setColor(new Color(0x1B5339));
        g.setStroke(new BasicStroke(Math.max(2f, n * 0.012f)));
        g.draw(new RoundRectangle2D.Double(1.5, 1.5, n - 3, n - 3, radius, radius));

        g.drawImage(glyphRaster(), 0, 0, n, n, null);
        g.dispose();
        return img;
    }

    /** Glyph drawn on an oversized transparent raster reproducing the launcher layout. */
    BufferedImage glyphRaster() {
        int n = 2000;
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        double glyphW = 78.0;
        double glyphH = 48.0;
        double scale = (n * 0.74) / glyphW;
        double ox = n / 2.0 - 54.0 * scale;
        double oy = n / 2.0 - 56.0 * scale;

        g.translate(ox, oy);
        g.scale(scale, scale);
        drawGlyph(g);
        g.dispose();
        return img;
    }

    BufferedImage socialPreview() throws Exception {
        int w = 1280, h = 640;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // Background
        GradientPaint grad = new GradientPaint(0, 0, new Color(0x0D2419), w, h, new Color(0x050A08));
        g.setPaint(grad);
        g.fillRect(0, 0, w, h);

        // Soft glow behind the glyph
        Color glow = new Color(0x1CD27F);
        for (int i = 0; i < 3; i++) {
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 10));
            g.fillOval(60 - i * 40, 70 - i * 30, 360 + i * 80, 500 + i * 60);
        }

        // App icon (user artwork), left side
        BufferedImage icon = ImageIO.read(new File("app/src/main/ic_launcher-playstore.png"));
        int badge = 380;
        int bx = 90;
        int by = (h - badge) / 2;
        g.setClip(new RoundRectangle2D.Double(bx, by, badge, badge, 76, 76));
        g.drawImage(icon, bx, by, badge, badge, null);
        g.setClip(null);

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 66));
        g.drawString("Battery Alarm", 620, 245);

        // Tagline
        g.setColor(new Color(0xB2FBD7));
        g.setFont(new Font("SansSerif", Font.PLAIN, 30));
        g.drawString("Avisa cuando tu celular termina de cargar.", 622, 305);

        // Pillars
        String[] pills = {"Gratis", "Open Source", "Sin publicidad"};
        int px = 620;
        int py = 348;
        g.setFont(new Font("SansSerif", Font.BOLD, 20));
        for (String p : pills) {
            FontMetrics fm = g.getFontMetrics();
            int pw = fm.stringWidth(p) + 34;
            int ph = 38;
            g.setColor(new Color(28, 210, 127, 26));
            g.fill(new RoundRectangle2D.Double(px, py, pw, ph, ph, ph));
            g.setColor(new Color(28, 210, 127, 160));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new RoundRectangle2D.Double(px, py, pw, ph, ph, ph));
            g.setColor(new Color(0xD8FCE9));
            g.drawString(p, px + 17, py + 26);
            px += pw + 14;
        }

        // Footer
        g.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g.setColor(new Color(0x7FC8A3));
        g.drawString("github.com/Norvyz/Battery-Alarm", 620, h - 60);

        g.dispose();
        return img;
    }

    void drawGlyph(Graphics2D g) {
        g.setColor(new Color(0xB2FBD7));

        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double nub = new Path2D.Double();
        nub.moveTo(50, 32);
        nub.lineTo(58, 32);
        nub.quadTo(60, 32, 60, 34);
        nub.lineTo(60, 38);
        nub.quadTo(60, 40, 58, 40);
        nub.lineTo(50, 40);
        nub.quadTo(48, 40, 48, 38);
        nub.lineTo(48, 34);
        nub.quadTo(48, 32, 50, 32);
        nub.closePath();
        g.draw(nub);

        g.setStroke(new BasicStroke(5.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double body = new Path2D.Double();
        body.moveTo(45, 40);
        body.lineTo(63, 40);
        body.quadTo(69, 40, 69, 46);
        body.lineTo(69, 74);
        body.quadTo(69, 80, 63, 80);
        body.lineTo(45, 80);
        body.quadTo(39, 80, 39, 74);
        body.lineTo(39, 46);
        body.quadTo(39, 40, 45, 40);
        body.closePath();
        g.draw(body);

        Path2D.Double bolt = new Path2D.Double();
        bolt.moveTo(56.5, 47);
        bolt.lineTo(47.5, 63);
        bolt.lineTo(53.2, 63);
        bolt.lineTo(52, 72);
        bolt.lineTo(61.5, 56.5);
        bolt.lineTo(55.8, 56.5);
        bolt.closePath();
        g.fill(bolt);

        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xB2FBD7));
        Path2D.Double a1 = new Path2D.Double();
        a1.moveTo(75, 51);
        a1.quadTo(81, 56, 81, 64);
        g.draw(a1);
        Path2D.Double a2 = new Path2D.Double();
        a2.moveTo(86, 43);
        a2.quadTo(93, 50, 93, 71);
        g.draw(a2);
        Path2D.Double a3 = new Path2D.Double();
        a3.moveTo(33, 51);
        a3.quadTo(27, 56, 27, 64);
        g.draw(a3);
        Path2D.Double a4 = new Path2D.Double();
        a4.moveTo(22, 43);
        a4.quadTo(15, 50, 15, 71);
        g.draw(a4);
    }
}