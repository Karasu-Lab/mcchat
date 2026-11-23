package com.karasu256.client.ui;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class SvgRasterizer {
  private static final Pattern COMMAND = Pattern.compile("([A-Za-z])([^A-Za-z]*)");
  private static final Pattern NUMBER =
      Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

  private SvgRasterizer() {}

  static NativeImage rasterize(InputStream svg, int size) throws IOException {
    try {
      Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg);
      Element root = document.getDocumentElement();
      String[] box = root.getAttribute("viewBox").trim().split("[\\s,]+");
      double viewWidth = Double.parseDouble(box[2]);
      double viewHeight = Double.parseDouble(box[3]);
      Path2D.Double shape = new Path2D.Double(Path2D.WIND_NON_ZERO);
      NodeList paths = root.getElementsByTagName("path");
      for (int i = 0; i < paths.getLength(); i++) {
        shape.append(parse(((Element) paths.item(i)).getAttribute("d")), false);
      }
      BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      Graphics2D graphics = image.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(
          RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
      graphics.transform(AffineTransform.getScaleInstance(size / viewWidth, size / viewHeight));
      graphics.setColor(Color.WHITE);
      graphics.fill(shape);
      graphics.dispose();
      NativeImage out = new NativeImage(NativeImage.Format.RGBA, size, size, false);
      for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {
          int argb = image.getRGB(x, y);
          out.setPixelRGBA(
              x,
              y,
              (argb & 0xFF000000)
                  | ((argb & 0xFF) << 16)
                  | (argb & 0xFF00)
                  | ((argb >> 16) & 0xFF));
        }
      }
      return out;
    } catch (Exception e) {
      throw new IOException("Cannot rasterize the SVG", e);
    }
  }

  private static Path2D.Double parse(String data) {
    Path2D.Double path = new Path2D.Double();
    double x = 0;
    double y = 0;
    double startX = 0;
    double startY = 0;
    double controlX = 0;
    double controlY = 0;
    Matcher commands = COMMAND.matcher(data);
    while (commands.find()) {
      char command = commands.group(1).charAt(0);
      boolean relative = Character.isLowerCase(command);
      double[] values = numbers(commands.group(2));
      int step =
          switch (Character.toUpperCase(command)) {
            case 'M', 'L' -> 2;
            case 'H', 'V' -> 1;
            case 'C' -> 6;
            case 'S', 'Q' -> 4;
            case 'Z' -> 0;
            default -> throw new IllegalArgumentException("Unsupported path command " + command);
          };
      for (int i = 0; step == 0 ? i == 0 : i + step <= values.length; i += Math.max(step, 1)) {
        double ox = relative ? x : 0;
        double oy = relative ? y : 0;
        switch (Character.toUpperCase(command)) {
          case 'M' -> {
            x = ox + values[i];
            y = oy + values[i + 1];
            if (i == 0) {
              path.moveTo(x, y);
              startX = x;
              startY = y;
            } else {
              path.lineTo(x, y);
            }
            controlX = x;
            controlY = y;
          }
          case 'L' -> {
            x = ox + values[i];
            y = oy + values[i + 1];
            path.lineTo(x, y);
            controlX = x;
            controlY = y;
          }
          case 'H' -> {
            x = ox + values[i];
            path.lineTo(x, y);
            controlX = x;
            controlY = y;
          }
          case 'V' -> {
            y = oy + values[i];
            path.lineTo(x, y);
            controlX = x;
            controlY = y;
          }
          case 'C' -> {
            controlX = ox + values[i + 2];
            controlY = oy + values[i + 3];
            path.curveTo(
                ox + values[i],
                oy + values[i + 1],
                controlX,
                controlY,
                ox + values[i + 4],
                oy + values[i + 5]);
            x = ox + values[i + 4];
            y = oy + values[i + 5];
          }
          case 'S' -> {
            double firstX = 2 * x - controlX;
            double firstY = 2 * y - controlY;
            controlX = ox + values[i];
            controlY = oy + values[i + 1];
            path.curveTo(
                firstX, firstY, controlX, controlY, ox + values[i + 2], oy + values[i + 3]);
            x = ox + values[i + 2];
            y = oy + values[i + 3];
          }
          case 'Q' -> {
            controlX = ox + values[i];
            controlY = oy + values[i + 1];
            path.quadTo(controlX, controlY, ox + values[i + 2], oy + values[i + 3]);
            x = ox + values[i + 2];
            y = oy + values[i + 3];
          }
          default -> {
            path.closePath();
            x = startX;
            y = startY;
            controlX = x;
            controlY = y;
          }
        }
      }
    }
    return path;
  }

  private static double[] numbers(String text) {
    Matcher matcher = NUMBER.matcher(text);
    double[] values = new double[text.length()];
    int count = 0;
    while (matcher.find()) {
      values[count++] = Double.parseDouble(matcher.group());
    }
    return java.util.Arrays.copyOf(values, count);
  }
}
