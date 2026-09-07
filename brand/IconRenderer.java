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
        double glyphW = 78.0;
        double glyphH = 48.0;
        double scale = (n * 0.74) / glyphW;
        double ox = n / 2.0 - 54.0 * scale;
        double oy = n / 2.0 - 56.0 * scale;

        g.translate(ox, oy);
        g.scale(scale, scale);

        drawGlyph(g, scale, scale);
        g.dispose();
        return img;
    }

    void drawGlyph(Graphics2D g, double s, double s2) {
        g.setColor(new Color(0xB2FBD7));

        // battery terminal
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

        // battery body
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

        // bolt (charging ray)
        Path2D.Double bolt = new Path2D.Double();
        bolt.moveTo(56.5, 47);
        bolt.lineTo(47.5, 63);
        bolt.lineTo(53.2, 63);
        bolt.lineTo(52, 72);
        bolt.lineTo(61.5, 56.5);
        bolt.lineTo(55.8, 56.5);
        bolt.closePath();
        g.fill(bolt);

        // sound arcs, symmetric both sides
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