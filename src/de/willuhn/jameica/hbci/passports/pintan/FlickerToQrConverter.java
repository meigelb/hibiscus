/**********************************************************************
 *
 * Copyright (c) 2022 Oezguer Emir
 * All rights reserved.
 * 
 * This software is copyrighted work licensed under the terms of the
 * Jameica License.  Please consult the file "LICENSE" for details. 
 *
 **********************************************************************/

package de.willuhn.jameica.hbci.passports.pintan;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.bind.DatatypeConverter;

import org.kapott.hbci.comm.Comm;

import io.nayuki.fastqrcodegen.QrCode;
import io.nayuki.fastqrcodegen.QrSegment;

/**
 * Konvertiert einen Flicker-Code in einen QR-Code.
 * Siehe https://raw.githubusercontent.com/oezgueremir/hibiscus/1df66169e093439ebaf75b0427fbd57008be8218/src/de/willuhn/jameica/hbci/passports/pintan/server/QRDataFromFlickerData.java
 */
public class FlickerToQrConverter
{
  // https://www.sunshine2k.de/coding/javascript/crc/crc_js.html
  // Params: CRC-16, Predefined: CRC16_ARC, Show reflected lookup table
  private static int[] c8005_table = new int[] { 0x0000, 0xC0C1, 0xC181, 0x0140, 0xC301, 0x03C0, 0x0280, 0xC241, 0xC601, 0x06C0, 0x0780, 0xC741, 0x0500, 0xC5C1,
      0xC481, 0x0440, 0xCC01, 0x0CC0, 0x0D80, 0xCD41, 0x0F00, 0xCFC1, 0xCE81, 0x0E40, 0x0A00, 0xCAC1, 0xCB81, 0x0B40, 0xC901, 0x09C0, 0x0880, 0xC841, 0xD801,
      0x18C0, 0x1980, 0xD941, 0x1B00, 0xDBC1, 0xDA81, 0x1A40, 0x1E00, 0xDEC1, 0xDF81, 0x1F40, 0xDD01, 0x1DC0, 0x1C80, 0xDC41, 0x1400, 0xD4C1, 0xD581, 0x1540,
      0xD701, 0x17C0, 0x1680, 0xD641, 0xD201, 0x12C0, 0x1380, 0xD341, 0x1100, 0xD1C1, 0xD081, 0x1040, 0xF001, 0x30C0, 0x3180, 0xF141, 0x3300, 0xF3C1, 0xF281,
      0x3240, 0x3600, 0xF6C1, 0xF781, 0x3740, 0xF501, 0x35C0, 0x3480, 0xF441, 0x3C00, 0xFCC1, 0xFD81, 0x3D40, 0xFF01, 0x3FC0, 0x3E80, 0xFE41, 0xFA01, 0x3AC0,
      0x3B80, 0xFB41, 0x3900, 0xF9C1, 0xF881, 0x3840, 0x2800, 0xE8C1, 0xE981, 0x2940, 0xEB01, 0x2BC0, 0x2A80, 0xEA41, 0xEE01, 0x2EC0, 0x2F80, 0xEF41, 0x2D00,
      0xEDC1, 0xEC81, 0x2C40, 0xE401, 0x24C0, 0x2580, 0xE541, 0x2700, 0xE7C1, 0xE681, 0x2640, 0x2200, 0xE2C1, 0xE381, 0x2340, 0xE101, 0x21C0, 0x2080, 0xE041,
      0xA001, 0x60C0, 0x6180, 0xA141, 0x6300, 0xA3C1, 0xA281, 0x6240, 0x6600, 0xA6C1, 0xA781, 0x6740, 0xA501, 0x65C0, 0x6480, 0xA441, 0x6C00, 0xACC1, 0xAD81,
      0x6D40, 0xAF01, 0x6FC0, 0x6E80, 0xAE41, 0xAA01, 0x6AC0, 0x6B80, 0xAB41, 0x6900, 0xA9C1, 0xA881, 0x6840, 0x7800, 0xB8C1, 0xB981, 0x7940, 0xBB01, 0x7BC0,
      0x7A80, 0xBA41, 0xBE01, 0x7EC0, 0x7F80, 0xBF41, 0x7D00, 0xBDC1, 0xBC81, 0x7C40, 0xB401, 0x74C0, 0x7580, 0xB541, 0x7700, 0xB7C1, 0xB681, 0x7640, 0x7200,
      0xB2C1, 0xB381, 0x7340, 0xB101, 0x71C0, 0x7080, 0xB041, 0x5000, 0x90C1, 0x9181, 0x5140, 0x9301, 0x53C0, 0x5280, 0x9241, 0x9601, 0x56C0, 0x5780, 0x9741,
      0x5500, 0x95C1, 0x9481, 0x5440, 0x9C01, 0x5CC0, 0x5D80, 0x9D41, 0x5F00, 0x9FC1, 0x9E81, 0x5E40, 0x5A00, 0x9AC1, 0x9B81, 0x5B40, 0x9901, 0x59C0, 0x5880,
      0x9841, 0x8801, 0x48C0, 0x4980, 0x8941, 0x4B00, 0x8BC1, 0x8A81, 0x4A40, 0x4E00, 0x8EC1, 0x8F81, 0x4F40, 0x8D01, 0x4DC0, 0x4C80, 0x8C41, 0x4400, 0x84C1,
      0x8581, 0x4540, 0x8701, 0x47C0, 0x4680, 0x8641, 0x8201, 0x42C0, 0x4380, 0x8341, 0x4100, 0x81C1, 0x8081, 0x4040 };

  /**
   * Konvertiert den Flicker-Code in einen QR-Code.
   * @param flicker der Flicker-Code.
   * @return der QR-Code.
   * @throws IOException
   */
  public static String convert(String flicker) throws IOException
  {
    byte[] data = DatatypeConverter.parseHexBinary(flicker);

    byte[] dk = new byte[] { (byte) (0x44), (byte) (0x4B) }; // DK
    byte[] ams = new byte[] { (byte) (0x4E) }; // N

    byte[] crc = crc16(new byte[][] { dk, ams, data });

    byte[] conv = scramble(dk, new byte[][] { ams, data, crc }, ams.length + data.length + crc.length);

    QrSegment segs = QrSegment.makeBytes(conv);
    QrSegment eci = QrSegment.makeEci(1);
    QrCode qr = QrCode.encodeSegments(Arrays.asList(eci, segs), QrCode.Ecc.LOW, 4, 8, -1, false);

    BufferedImage img = toImage(qr, 4, 2);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
    ImageIO.write(img, "png", stream);
    baos.flush();

    // Logger.debug("PNG hexdump: " + javax.xml.bind.DatatypeConverter.printHexBinary(baos.toByteArray()));

    StringBuilder sb = new StringBuilder();

    String buf = "image/png";
    int len = buf.length();
    sb.append((char) (len >>> 8));
    sb.append((char) (len));
    sb.append(buf);

    buf = baos.toString(Comm.ENCODING);
    len = buf.length();
    sb.append((char) (len >>> 8));
    sb.append((char) (len));
    sb.append(buf);

    return sb.toString();
  }

  /**
   * Konvertiert den QR-Code in ein Bild.
   * @param qr der QR-Code.
   * @param scale die Skalierung.
   * @param border die Rahmenbreite.
   * @return das erstellte Bild.
   */
  private static BufferedImage toImage(QrCode qr, int scale, int border)
  {
    if (scale <= 0 || border < 0)
      throw new IllegalArgumentException("Value out of range");
    if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale)
      throw new IllegalArgumentException("Scale or border too large");

    int width = (qr.size + border * 2) * scale;
    int height = width;
    int borderoffset = border * scale;

    BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);

    byte[] bankData = ((DataBufferByte) result.getRaster().getDataBuffer()).getBankData()[0];
    Arrays.fill(bankData, (byte) 0xff);// white

    // byte[] bankFrame = new byte[(width+7)/8];

    for (int y = borderoffset; y < height - borderoffset; ++y)
    {
      for (int x = borderoffset; x < width - borderoffset; ++x)
      {
        if (qr.getModule(x / scale - border, y / scale - border))
          result.setRGB(x, y, 0x00);// black
      }
    }

    return result;
  }

  /**
   * Fuehrt das Scrambling des QR-Codes aus.
   * @param scrambler
   * @param data
   * @param dataLen
   * @return
   */
  private static byte[] scramble(byte[] scrambler, byte[][] data, int dataLen)
  {
    int scramblerLen = scrambler.length;

    byte[] result = new byte[scrambler.length + dataLen];
    System.arraycopy(scrambler, 0, result, 0, scramblerLen);

    int p = 0;
    for (byte[] buf : data)
    {
      for (byte c : buf)
      {
        result[scramblerLen+p] = (byte) (c ^ scrambler[p++ % scramblerLen]);
      }
    }

    return result;
  }

  /**
   * Erzeugt eine CRC16-Pruefsumme.
   * @param data die Daten.
   * @return die Pruefsumme.
   */
  private static byte[] crc16(byte[][] data)
  {
    int crc = 0x0000;

    for (byte[] buf : data)
    {
      for (byte c : buf)
      {
        crc = (crc >>> 8) ^ c8005_table[(crc ^ c) & 0x00FF];
      }
    }

    return new byte[] { (byte) ((crc >>> 8) & 0xFF), (byte) (crc & 0xFF) };
  }
}
