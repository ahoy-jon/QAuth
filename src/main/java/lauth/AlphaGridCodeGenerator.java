package lauth;


import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

public class AlphaGridCodeGenerator {
    private static ByteMatrix generateMatrix(final String data, final ErrorCorrectionLevel level) throws WriterException {
        final QRCode qr = Encoder.encode(data, level);
        final ByteMatrix matrix = qr.getMatrix();
        return matrix;
    }

    private static void writeImage(final OutputStream os, final String imageFormat, final ByteMatrix matrix, final int size, final int foregroundColor)
            throws FileNotFoundException, IOException {

        /**
         * Java 2D Traitement de Area
         */
        Area a = new Area(); // les futurs modules
        Area module = new Area(new Rectangle2D.Float(0.05f, 0.05f, 1f, 1f));

        AffineTransform at = new AffineTransform(); // pour deplacer le module
        int width = matrix.getWidth();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < width; j++) {
                if (matrix.get(j, i) == 1) {
                    a.add(module); // on ajoute le module
                }
                at.setToTranslation(1, 0); // on decale vers la droite
                module.transform(at);
            }
            at.setToTranslation(-width, 1); // on saute une ligne on revient au
            module.transform(at);
        }

        // agrandissement de l'Area pour le remplissage de l'image
        double ratio = size / (double) width;
        // il faut respecter la Quietzone : 4 modules de bordures autour du QR
        // Code
        double adjustment = width / (double) (width + 2);
        ratio = ratio * adjustment;

        at.setToTranslation(1, 1); // a cause de la quietzone
        a.transform(at);

        at.setToScale(ratio, ratio); // on agrandit
        a.transform(at);

        /**
         * Java 2D Traitement l'image
         */
        BufferedImage im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) im.getGraphics();


        /*int rule = AlphaComposite.SRC_OVER;
        Composite comp = AlphaComposite.getInstance(rule ,0);
        g.setComposite(comp);
*/
        g.setBackground(new Color(0,0,0,0));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f));

        //g.setBackground(new Color(0x00FFFFFF, true));
        g.clearRect(0, 0, size, size); // pour le fond blanc

        Color couleur1 = new Color(0xFFFF0000, true);
        g.setPaint(couleur1);

        g.fill(a); // remplissage des modules
        // Ecriture sur le disque

        try {
            ImageIO.write(im, imageFormat, os);
        } catch (Exception e) {
        }

    }


    public static void encodeData(String data, OutputStream os, int foregroundColor) throws IOException, WriterException {
        final int size = 400;
        final ErrorCorrectionLevel level = ErrorCorrectionLevel.L;
        final String imageFormat = "png";
        final ByteMatrix matrix = generateMatrix(data, level);

        writeImage(os, imageFormat, matrix, size, foregroundColor);

    }


}