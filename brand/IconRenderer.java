import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;

/**
 * Renders the Battery Alarm launcher icon (legacy PNG mipmaps) using the same
 * geometry as brand/logo.svg, so all densities match the master artwork.
 * Ordinary icon (rounded square) + round icon (circular mask).
 */
public class IconRenderer {

    static final int[] SIZES = {48, 72, 96, 144, 192};

    public static void main(String[] args) throws Exception {
        IconRenderer r = new IconRenderer();
        File base = new File("app/src/main/res");
        for (int size : SIZES) {
            String density = densityFor(size);
            File dir = new File(base, "mipmap-" + density);
            Files.createDirectories(dir.toPath());
            ImageIO.write(r.render(size, false), "png", new File(dir, "ic_launcher.png"));
            ImageIO.write(r.render(size, true), "png", new File(dir, "ic_launcher_round.png"));
            System.out.println("wrote " + dir);
        }
    }

    static String densityFor(int size) {
        switch (size) {
            case 48: return "mdpi";
            case 72: return "hdpi";
            case 96: return "xhdpi";
            case 144: return "xxhdpi";
            case 192: return "xxxhdpi";
        }
        throw new IllegalArgumentException();
    }

    BufferedImage render(int n, boolean round) {
        BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Background
        if (round) {
            g.setColor(new Color(0x0D2419));
            g.fill(new Ellipse2D.Double(0, 0, n, n));
            // soft ring
            g.setStroke(new BasicStroke(Math.max(1, n * 0.02f)));
            g.setColor(new Color(0x1B5339));
            g.draw(new Ellipse2D.Double(1, 1, n - 2, n - 2));
        } else {
            GradientPaint grad = new GradientPaint(0, 0, new Color(0x123126), 0, n, new Color(0x060C09));
            g.setPaint(grad);
            float radius = n * 0.22f;
            g.fill(new RoundRectangle2D.Double(0, 0, n, n, radius, radius));
        }

        // Glyph scale from 108-box geometry
        double glyphW = 65.0;
        double glyphH = 79.0;
        double scale = (n * 0.66) / glyphW;
        double gh = glyphH * scale;
        double gw = glyphW * scale;
        double ox = (n - gw) / 2.0;
        double oy = (n - gh) / 2.0;

        g.translate(ox, oy);
        g.scale(scale, scale);

        drawGlyph(g, n * 0.66 / 65.0 > 0 ? scale : scale, scale);
        g.dispose();
        return img;
    }

    void drawGlyph(Graphics2D g, double s, double s2) {
        // sound arcs
        g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xB2FBD7));
        Path2D.Double arc1 = new Path2D.Double();
        arc1.moveTo(80, 16);
        arc1.quadTo(88, 24, 88, 32);
        g.draw(arc1);
        Path2D.Double arc2 = new Path2D.Double();
        arc2.moveTo(90, 9);
        arc2.quadTo(100, 20, 100, 34);
        g.draw(arc2);

        // bell
        g.setColor(new Color(0x54F5A0));
        Path2D.Double bell = new Path2D.Double();
        bell.moveTo(40, 26);
        bell.curveTo(44, 17, 64, 17, 68, 26);
        bell.lineTo(73, 40);
        bell.curveTo(64, 47, 44, 47, 35, 40);
        bell.closePath();
        g.fill(bell);

        // clapper
        g.setColor(new Color(0xB2FBD7));
        g.fill(new Ellipse2D.Double(54 - 3.2, 44 - 3.2, 6.4, 6.4));

        // battery body stroke
        g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0xF2FFF8));
        Path2D.Double body = new Path2D.Double();
        body.moveTo(45, 48);
        body.lineTo(70, 48);
        body.curveTo(75, 48, 79, 52, 79, 57);
        body.lineTo(79, 79);
        body.curveTo(79, 84, 75, 88, 70, 88);
        body.lineTo(45, 88);
        body.curveTo(40, 88, 36, 84, 36, 79);
        body.lineTo(36, 57);
        body.curveTo(36, 52, 40, 48, 45, 48);
        body.closePath();
        g.draw(body);

        // charge segments
        g.setColor(new Color(0x2FE68F));
        g.fill(new RoundRectangle2D.Double(40, 52, 34, 10, 4.5, 4.5));
        g.setColor(new Color(0x1BD17F));
        g.fill(new RoundRectangle2D.Double(40, 64, 34, 10, 4.5, 4.5));
        g.setColor(new Color(0x0FAE68));
        g.fill(new RoundRectangle2D.Double(40, 76, 34, 7, 3, 3));

        // bolt
        g.setColor(Color.WHITE);
        Path2D.Double bolt = new Path2D.Double();
        bolt.moveTo(54, 59);
        bolt.lineTo(50, 67);
        bolt.lineTo(53.5, 67);
        bolt.lineTo(52, 75);
        bolt.lineTo(58, 65);
        bolt.lineTo(54.5, 65);
        bolt.closePath();
        g.fill(bolt);
    }
}