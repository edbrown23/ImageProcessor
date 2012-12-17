package ImageProcessing;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

/**
 * This software falls under the MIT license, as follows:
 * Copyright (C) 2012
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * <p/>
 * Created By: Eric Brown
 * Date: 12/8/12
 */
public class Image extends BufferedImage {

    public Image(int width, int height, int imageType) {
        super(width, height, imageType);
    }

    public Image(Image toCopy){
        super(toCopy.getWidth(), toCopy.getHeight(), toCopy.getType());
        for(int y = 0; y < toCopy.getHeight(); y++){
            for(int x = 0; x < toCopy.getWidth(); x++){
                this.setRGB(x, y, toCopy.getRGB(x, y));
            }
        }
    }

    public Image(BufferedImage toCopy){
        super(toCopy.getWidth(), toCopy.getHeight(), toCopy.getType());
        for(int y = 0; y < toCopy.getHeight(); y++){
            for(int x = 0; x < toCopy.getWidth(); x++){
                this.setRGB(x, y, toCopy.getRGB(x, y));
            }
        }
    }

    public static Image convertToGrayScale(Image img) {
        Image outputImage = new Image(img.getWidth(), img.getHeight(), img.getType());
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int value = img.getRGB(x, y);
                int red = (value & 0xff0000) >> 16;
                int green = (value & 0xff00) >> 8;
                int blue = value & 0xff;
                int newValue = (red + green + blue) / 3;
                newValue = 0xff000000 | (newValue << 16 | newValue << 8 | newValue);
                outputImage.setRGB(x, y, newValue);
            }
        }
        return outputImage;
    }

    /**
     * Finds edges with the x and y Sobel Filters. Destroys the original image
     * Sobel X Filter:
     * -1  0  1
     * -2  0  2
     * -1  0  1
     * Sobel Y Filter:
     * -1 -2 -1
     * 0  0  0
     * 1  2  1
     */
    public static Image detectEdgesSobel(Image sourceImage) {
        float[][] ySobel = {{-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}};
        float[][] xSobel = {{-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}};
        Image xCopy = convolveImage(sourceImage, xSobel, BoundaryPolicies.None);
        Image yCopy = convolveImage(sourceImage, ySobel, BoundaryPolicies.None);
        return addImages(xCopy, yCopy);
    }

    public static Image addImages(Image img1, Image img2) {
        Image output = new Image(img1.getWidth(), img1.getHeight(), img1.getType());
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                int oldVal = img1.getRGB(x, y) & 0xff;
                int newVal = img2.getRGB(x, y) & 0xff;
                newVal += oldVal;
                if (newVal > 255) {
                    newVal = 255;
                }
                newVal = 0xff000000 | (newVal << 16 | newVal << 8 | newVal);
                output.setRGB(x, y, newVal);
            }
        }
        return output;
    }

    /**
     * Detects edges with the Canny Edge Detector. This method is deliberately unoptimized to be demonstrative
     *
     * @param sigma         The Gaussian Kernel sigma
     * @param lowThreshold  The lower threshold for the hysteresis step
     * @param highThreshold The upper threshold for the hysteresis step
     */
    public static Image detectEdgesCanny(Image sourceImage, float sigma, int lowThreshold, int highThreshold) {
        Image grayscale = convertToGrayScale(sourceImage);
        // smooth the image with the gaussian kernel
        float[][] kernel = generateGaussianKernel(sigma);
        Image smoothedImage = convolveImage(grayscale, kernel, BoundaryPolicies.None);

        // Acquire the image gradients
        float[][] ySobel = {{1, 0, -1},
                {2, 0, -2},
                {1, 0, -1}};
        float[][] xSobel = {{-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}};
        Image gX = convolveImage(smoothedImage, xSobel, BoundaryPolicies.None);
        Image gY = convolveImage(smoothedImage, ySobel, BoundaryPolicies.None);

        // Determine image angles
        Image angles = calculateImageAngles(gX, gY);

        Image gradient = calculateGradientImage(gX, gY);

        Image nonMax = calculateNonMaximalSuppression(gradient, angles);

        Image hyst = applyHysteresis(nonMax, lowThreshold, highThreshold);

        return hyst;
    }

    public static Image applyHysteresis(Image nonMax, int lThresh, int hThresh) {
        Image output = new Image(nonMax.getWidth(), nonMax.getHeight(), nonMax.getType());
        boolean changed = true;
        int count = 0;
        for (int y = 0; y < nonMax.getHeight(); y++) {
            for (int x = 0; x < nonMax.getWidth(); x++) {
                if(nonMax.getGrayScalePixel(x, y) > hThresh){
                    output.setRGB(x, y, 0xffffffff);
                }else if(nonMax.getGrayScalePixel(x, y) < lThresh){
                    output.setRGB(x, y, 0xff000000);
                }
            }
        }
        while (changed) {
            count++;
            if(count > 100){
                break;
            }
            changed = false;
            for (int y = 1; y < output.getHeight() - 1; y++) {
                for (int x = 1; x < output.getWidth() - 1; x++) {
                    if(nonMax.getGrayScalePixel(x, y) > lThresh && nonMax.getGrayScalePixel(x, y) < hThresh){
                        for(int j = -1; j < 2; j++){
                            for(int i = -1; i < 2; i++){
                                if(nonMax.getGrayScalePixel(x + i, y + j) > hThresh){
                                    output.setRGB(x, y, 0xffffffff);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println(count);
        return output;
    }

    public static Image calculateGradientImage(Image gX, Image gY) {
        Image output = new Image(gX.getWidth(), gX.getHeight(), gX.getType());
        for (int y = 0; y < gX.getHeight(); y++) {
            for (int x = 0; x < gX.getWidth(); x++) {
                // somewhat inefficient euclidean distance. Could use manhattan distance for some extra speed
                int xSquared = gX.getGrayScalePixel(x, y) * gX.getGrayScalePixel(x, y);
                int ySquared = gY.getGrayScalePixel(x, y) * gY.getGrayScalePixel(x, y);
                int newVal = (int) Math.round(Math.sqrt(xSquared + ySquared));
                if(newVal > 255){
                    newVal = 255;
                }
                newVal = 0xff000000 | (newVal << 16 | newVal << 8 | newVal);
                output.setRGB(x, y, newVal);
            }
        }
        return output;
    }

    public static Image calculateNonMaximalSuppression(Image gradientImage, Image angleImage) {
        Image output = new Image(gradientImage);
        int nonMaxCount = 0;
        for (int y = 1; y < gradientImage.getHeight() - 1; y++) {
            for (int x = 1; x < gradientImage.getWidth() - 1; x++) {
                // Get the appropriate neighbor intensities, based on the current pixel's angle
                int firstNeighbor = 0;
                int secondNeighbor = 0;
                int currentPixel = gradientImage.getGrayScalePixel(x, y);
                if (angleImage.getGrayScalePixel(x, y) == 0) {
                    firstNeighbor = gradientImage.getGrayScalePixel(x - 1, y);
                    secondNeighbor = gradientImage.getGrayScalePixel(x + 1, y);
                } else if (angleImage.getGrayScalePixel(x, y) == 90) {
                    firstNeighbor = gradientImage.getGrayScalePixel(x, y - 1);
                    secondNeighbor = gradientImage.getGrayScalePixel(x, y + 1);
                } else if (angleImage.getGrayScalePixel(x, y) == 45) {
                    firstNeighbor = gradientImage.getGrayScalePixel(x + 1, y + 1);
                    secondNeighbor = gradientImage.getGrayScalePixel(x - 1, y - 1);
                } else if (angleImage.getGrayScalePixel(x, y) == 135) {
                    firstNeighbor = gradientImage.getGrayScalePixel(x + 1, y - 1);
                    secondNeighbor = gradientImage.getGrayScalePixel(x - 1, y + 1);
                }
                // If the current pixel is not a local max, remove it from the image
                if (currentPixel < firstNeighbor || currentPixel < secondNeighbor) {
                    output.setRGB(x, y, 0xff000000);
                }
            }
        }
        return output;
    }

    private static Image calculateImageAngles(Image gX, Image gY) {
        Image output = new Image(gX.getWidth(), gX.getHeight(), gX.getType());
        for (int y = 0; y < gX.getHeight(); y++) {
            for (int x = 0; x < gY.getWidth(); x++) {
                int newValue = 0;
                float angle;
                // To avoid divide by zeroes, we set the angle appropriately here
                if (gX.getGrayScalePixel(x, y) == 0) {
                    if (gY.getGrayScalePixel(x, y) == 0) {
                        newValue = 0;
                    } else {
                        newValue = 90;
                    }
                } else {
                    // We determine the angle as below, then round the angle to 0, 45, 90, or 135, depending on what's closest
                    float gradY = gY.getGrayScalePixel(x, y);
                    float gradX = gX.getGrayScalePixel(x, y);
                    angle = (float) Math.toDegrees(Math.atan(gradY / gradX));
                    if (angle >= 157.5 || angle < 22.5) {
                        newValue = 0;
                    } else if (angle >= 22.5 && angle < 67.5) {
                        newValue = 135;
                    } else if (angle >= 67.5 && angle < 112.5) {
                        newValue = 90;
                    } else if (angle >= 112.5 && angle < 157.5) {
                        newValue = 45;
                    }
                }
                newValue = 0xff000000 | (newValue << 16) | (newValue << 8) | newValue;
                output.setRGB(x, y, newValue);
            }
        }
        return output;
    }

    public int[][] toGrayScaleArray() {
        int[][] values = new int[getWidth()][getHeight()];
        for (int y = 0; y < getHeight(); y++) {
            for (int x = 0; x < getWidth(); x++) {
                values[x][y] = getRGB(x, y) & 0xff;
            }
        }
        return values;
    }

    public static float[][] generateGaussianKernel(float sigma) {
        float[][] gaussianKernel = new float[5][5];

        double sigmaSquared = sigma * sigma;
        double firstTerm = 1.0d / (2d * Math.PI * sigmaSquared);
        for (int j = 0; j < 5; j++) {
            for (int i = 0; i < 5; i++) {
                int x = i - 2;
                int y = j - 2;
                gaussianKernel[i][j] = (float) (firstTerm * Math.exp(-1d * (((x * x) + (y * y)) / 2 * sigmaSquared)));
            }
        }
        return gaussianKernel;
    }

    public void convolveImage(Image sourceImage, float[][] filter) {
        convolveImage(sourceImage, filter, BoundaryPolicies.None);
    }

    public static Image convolveImage(Image sourceImage, float[][] filter, BoundaryPolicies p) {
        Image output = new Image(sourceImage.getWidth(), sourceImage.getHeight(), sourceImage.getType());
        int filterWidth = filter[0].length;
        int filterHeight = filter.length;
        float fSum = getFilterSum(filter);
        if (Math.abs(fSum) < 0.0001) {
            fSum = 1;
        }

        int fW = filterWidth / 2;
        int fH = filterHeight / 2;
        for (int y = 0; y < sourceImage.getHeight(); y++) {
            for (int x = 0; x < sourceImage.getWidth(); x++) {
                float filterSum = 0;
                for (int fY = -1 * fH; fY <= fH; fY++) {
                    for (int fX = -1 * fW; fX <= fW; fX++) {
                        int iX = x + fX;
                        int iY = y + fY;
                        int pixelValue = 0;
                        if (iX < 0 || iX >= sourceImage.getWidth() || iY < 0 || iY >= sourceImage.getHeight()) {
                            if (p == BoundaryPolicies.None) {
                                break;
                            }
                        } else {
                            // We're really only supporting grayscale images right now, so we just need one color channel
                            pixelValue = sourceImage.getRGB(iX, iY) & 0xff;
                        }
                        filterSum += (float) pixelValue * filter[fX + fW][fY + fH];
                    }
                }
                // Once again, we're just doing grayscale for now
                int filterTotal = Math.abs(Math.round(filterSum / fSum));
                if (filterTotal > 255) {
                    filterTotal = 255;
                }
                int newPixelValue = 0xff000000 | (filterTotal << 16 | filterTotal << 8 | filterTotal);
                output.setRGB(x, y, newPixelValue);
            }
        }
        return output;
    }

    public static float getFilterSum(float[][] filter) {
        float sum = 0;
        for (int y = 0; y < filter.length; y++) {
            for (int x = 0; x < filter[0].length; x++) {
                sum += filter[x][y];
            }
        }
        return sum;
    }

    public int getGrayScalePixel(int x, int y) {
        return getRGB(x, y) & 0xff;
    }
}
