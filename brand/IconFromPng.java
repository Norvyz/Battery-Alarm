import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Adopts app/src/main/ic_launcher-playstore.png as the official app icon. */
public class IconFromPng {
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File("app/src/main/ic_launcher-playstore.png"));

        Object[][] mipmaps = {
            {48, "mdpi"}, {72, "hdpi"}, {96, "xhdpi"}, {144, "xxhdpi"}, {192, "xxxhdpi"}
        };
        for (Object[] m : mipmaps) {
            File dir = new File("app/src/main/res/mipmap-" + m[1]);
            ImageIO.write(scale(src, (Integer) m[0]), "png", new File(dir, "ic_launcher.png"));
            System.out.println("wrote app/src/main/res/mipmap-" + m[1]);
        }

        // Adaptive foreground: artwork scaled to 65% so the rounded square fits
        // inside the circular launcher safe zone without clipping.
        int n = 1080;
        double f = 0.65;
        BufferedImage fg = new BufferedImage(n, n, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gg = fg.createGraphics();
        gg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = (int) Math.round(n * f);
        int o = (n - size) / 2;
        gg.drawImage(src, o, o, size, size, null);
        gg.dispose();
        File nodpi = new File("app/src/main/res/drawable-nodpi");
        nodpi.mkdirs();
        ImageIO.write(fg, "png", new File(nodpi, "ic_launcher_fg.png"));
        System.out.println("wrote app/src/main/res/drawable-nodpi/ic_launcher_fg.png");

        // Repo assets (same artwork)
        ImageIO.write(scale(src, 512), "png", new File("assets/icon-512.png"));
        ImageIO.write(scale(src, 192), "png", new File("assets/icon-192.png"));
        System.out.println("wrote assets/icon-512.png, assets/icon-192.png");
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
}