package ImageProcessing;

import com.sun.deploy.panel.ITreeNode;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import java.awt.image.BufferedImage;
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
public class Image{
    private BufferedImage image;
    private int width;
    private int height;

    /**
     * Constructs a new image which is a copy of the input image
     * @param srcImage The image to copy
     */
    public Image(BufferedImage srcImage){
        image = srcImage;
        this.width = srcImage.getWidth();
        this.height = srcImage.getHeight();
    }

    /**
     * Constructs a new image which is loaded from a file
     * @param imgFileName The filename/location from which to load the image
     */
    public Image(String imgFileName){
        try{
            image = ImageIO.read(new File(imgFileName));
            this.width = image.getWidth();
            this.height = image.getHeight();
        }catch(IOException e){
            System.err.println(e.getMessage());
        }
    }

    /**
     * Constructs an empty image of the desired type.
     * @param width Desired Image Width
     * @param height Desired Image Height
     * @param imageType The image type. Constants can be found as static fields of the BufferedImage class
     */
    public Image(int width, int height, int imageType){
        image = new BufferedImage(width, height, imageType);
        this.width = width;
        this.height = height;
    }

    public void convertToGrayScale(){
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                int value = image.getRGB(x, y);
                int red = (value & 0xff0000) >> 16;
                int green = (value & 0xff00) >> 8;
                int blue = value & 0xff;
                int newValue = (red + green + blue) / 3;
                newValue = 0xff000000 | (newValue << 16 | newValue << 8 | newValue);
                image.setRGB(x, y, newValue);
            }
        }
    }

    /**
     * Finds edges with the x and y Sobel Filters. Destroys the original image
     * Sobel X Filter:
     *      -1  0  1
     *      -2  0  2
     *      -1  0  1
     * Sobel Y Filter:
     *      -1 -2 -1
     *       0  0  0
     *       1  2  1
     */
    public void detectEdgesSobel(){

    }

    public void detectEdgesCanny(float sigma, int lowThreshold, int highThreshold){
        float[][] kernel = generateGaussianKernel(sigma);
        convolveImage(kernel);
    }

    public float[][] generateGaussianKernel(float sigma){
        float[][] gaussianKernel = new float[5][5];

        double sigmaSquared = sigma * sigma;
        double firstTerm = 1.0d / (2d * Math.PI * sigmaSquared);
        for(int j = 0; j < 5; j++){
            for(int i = 0; i < 5; i++){
                int x = i - 2;
                int y = j - 2;
                gaussianKernel[i][j] = (float)(firstTerm * Math.exp(-1d * (((x * x) + (y * y)) / 2 * sigmaSquared)));
            }
        }
        return gaussianKernel;
    }

    public void convolveImage(float[][] filter){
        convolveImage(filter, BoundaryPolicies.None);
    }

    public void convolveImage(float[][] filter, BoundaryPolicies p){
        BufferedImage output = new BufferedImage(width, height, image.getType());
        int filterWidth = filter[0].length;
        int filterHeight = filter.length;
        float fSum = getFilterSum(filter);
        if(Math.abs(fSum) < 0.0001){
            fSum = 1;
        }

        int fW = filterWidth / 2;
        int fH = filterHeight / 2;
        for(int y = 0; y < height; y++){
            for(int x = 0; x < width; x++){
                float filterSum = 0;
                for(int fY = -1 * fH; fY <= fH; fY++){
                    for(int fX = -1 * fW; fX <= fW; fX++){
                        int iX = x + fX;
                        int iY = y + fY;
                        int pixelValue = 0;
                        if(iX < 0 || iX >= width || iY < 0 || iY >= height){
                            if(p == BoundaryPolicies.None){
                                break;
                            }
                        }else{
                            // We're really only supporting grayscale images right now, so we just need one color channel
                            pixelValue = image.getRGB(iX, iY) & 0xff;
                        }
                        filterSum += (float)pixelValue * filter[fX + fW][fY + fH];
                    }
                }
                // Once again, we're just doing grayscale for now
                int filterTotal = Math.round(filterSum / fSum);
                if(filterTotal > 255){
                    filterTotal = 255;
                }else if(filterTotal < 0){
                    filterTotal = 0;
                }
                int newPixelValue = 0xff000000 | (filterTotal << 16 | filterTotal << 8 | filterTotal);
                output.setRGB(x, y, newPixelValue);
            }
        }
        image = output;
    }

    public float getFilterSum(float[][] filter){
        float sum = 0;
        for(int y = 0; y < filter.length; y++){
            for(int x = 0; x < filter[0].length; x++){
                sum += filter[x][y];
            }
        }
        return sum;
    }

    public int getHeight(){
        return height;
    }

    public int getWidth(){
        return width;
    }

    public BufferedImage getImage(){
        return image;
    }
}
