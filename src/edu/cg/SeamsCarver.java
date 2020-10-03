package edu.cg;
import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.ArrayList;
import java.util.stream.IntStream;

@SuppressWarnings("ALL")
public class SeamsCarver extends ImageProcessor {

    // MARK: fields
    private final int numOfSeams;
    private final ResizeOperation resizeOp;
    BufferedImage Scale_grey_Image;
    boolean[][] imageMask;

    // Our fields:

    boolean[][] new_Mask;
    boolean[][] orig_Mask;
    long[][] energyMat;
    long[][] costMat;
    int[][] IndicesMat;
    int[][] SeamsInd_Mat_final;
    int Width_curr;
    int Count_S_remove;
    ArrayList < int[] > SeamsIndices_final;

    public SeamsCarver(Logger logger, BufferedImage workingImage, int outWidth, RGBWeights rgbWeights,
                       boolean[][] imageMask) {
        super ((s) -> logger.log ("Seam carving: " + s), workingImage, rgbWeights, outWidth, workingImage.getHeight ( ));

        numOfSeams = Math.abs (outWidth - inWidth);
        this.imageMask = imageMask;
        if (inWidth < 2 | inHeight < 2)
            throw new RuntimeException ("Can not apply seam carving: workingImage is too small");

        if (numOfSeams > inWidth / 2)
            throw new RuntimeException ("Can not apply seam carving: too many seams...");

        // Setting resizeOp by with the appropriate method reference
        if (outWidth > inWidth)
            resizeOp = this::increaseImageWidth;
        else if (outWidth < inWidth)
            resizeOp = this::reduceImageWidth;
        else
            resizeOp = this::duplicateWorkingImage;

        // Our additional fields initialization:
        this.SeamsIndices_final = new ArrayList <> ( );
        this.Count_S_remove = 0;
        this.Width_curr = inWidth;
        this.Scale_grey_Image = this.greyscale ( );
        this.SeamsInd_Mat_final = new int[this.numOfSeams][this.inHeight];
        this.orig_Mask = imageMask;
        this.calc_Energy_Mat ( ); // calculates the the energy table.
        this.calc_Cost_Mat ( ); // calculates the cost matrix for the image
        this.T_IndicesMat_calc ( );

        this.logger.log("initialization finished");
    }

    public BufferedImage resize() {
        return resizeOp.resize ( );
    }


    /**
     * Reduces the image size.
     * @return the reduced size image
     */
    private BufferedImage reduceImageWidth( ) {

        logger.log ("Preparing the image for width reduction...");
        BufferedImage Img_reduce;
        Img_reduce = this.workingImage;
        for (int i = this.numOfSeams - 1; i >= 0; i--) {
            Img_reduce = BestSeam_del (Img_reduce);
            this.calc_Energy_Mat ( );
            this.calc_Cost_Mat ( );
        }
        logger.log("reducingImageWidth done!");
        return Img_reduce;
    }

    /**
     * Increases the image size .
     * @return the increased size image
     */
    private BufferedImage increaseImageWidth( ) {
        this.logger.log ("begins increasing image size");
        reduceImageWidth ( );
        BufferedImage Img_increse; // Create new image with addition sream we need
        this.Width_curr = this.inWidth;
        Img_increse = new BufferedImage (this.outWidth, this.outHeight, this.workingImageType);
        this.new_Mask = new boolean[this.outHeight][this.outWidth];
        for (int row = this.inHeight - 1; row >= 0; row--) {
            int num_s_r = 0;
            int col;
            for (col = 0; col < this.inWidth; col++) {
                int currIdx;
                currIdx = col + num_s_r;
                Img_increse.setRGB (currIdx, row, this.workingImage.getRGB (col, row));
                if (this.orig_Mask[row][col]) {
                    new_Mask[row][col] = true;
                } else {
                    new_Mask[row][col] = false;
                }
                int Seam;
                for (Seam = 0; Seam < this.SeamsInd_Mat_final.length; Seam++) {
                    if (this.SeamsIndices_final.get (Seam)[row] != col) {
                        continue;
                    }
                    int curr_color;
                    curr_color = this.workingImage.getRGB (col, row);
                    Img_increse.setRGB (col + (++num_s_r), row, curr_color);
                    if (this.orig_Mask[row][col]) new_Mask[row][col + num_s_r] = true;
                    else new_Mask[row][col + num_s_r] = false;
                }
            }
        }
        this.imageMask = new_Mask;
        logger.log("Image width increase done!");
        return Img_increse;
    }

    /**
     *
     * @param seamColorRGB color to highlight the seams.
     * @return a copy of the original image with the found seams highlighted
     */
    public BufferedImage showSeams(int seamColorRGB) {
        logger.log("Preparing the image for seam marking...");
        BufferedImage Img_seam_S;
        Img_seam_S = duplicateWorkingImage ( );
        reduceImageWidth ( );
        for (int i = this.SeamsInd_Mat_final.length - 1; i >= 0; i--) {
            int[] best_curr_Seam;
            best_curr_Seam = this.SeamsIndices_final.get (i);
            IntStream.range (0, this.inHeight).forEach (j -> {
                int x = best_curr_Seam[j];
                Img_seam_S.setRGB (x, j, seamColorRGB);
            });
        }
        this.logger.log ("showSeams Image done.");
        return Img_seam_S;
    }

    public boolean[][] getMaskAfterSeamCarving( ) {
        // This method should return the mask of the resize image after seam carving.
        // Meaning, after applying Seam Carving on the input image,
        // getMaskAfterSeamCarving() will return a mask, with the same dimensions as the
        // resized image, where the mask values match the original mask values for the
        // corresponding pixels.
        // HINT: Once you remove (replicate) the chosen seams from the input image, you
        // need to also remove (replicate) the matching entries from the mask as well.
        if (this.new_Mask == null) {
            return this.imageMask;
        } else {
            return this.new_Mask;
        }
    }



   //Calculate the costs matrix according to the formula
    private void calc_Cost_Mat( ) {
        this.pushForEachParameters ( );
        this.setForEachParameters (Width_curr, inHeight);
        this.costMat = new long[inHeight][Width_curr];

        forEach (this::accept);

        this.popForEachParameters ( );
    }

    // calculates the the energy table.
    private void calc_Energy_Mat( ) {
        this.pushForEachParameters ( );
        this.setForEachParameters (this.Width_curr, this.inHeight);
        this.energyMat = new long[this.inHeight][this.Width_curr];
        forEach (this::accept2);
        this.popForEachParameters ( );
    }

    private int c_right(int x, int y) {
        int c1;
        int c2;
        c1 = new Color (this.Scale_grey_Image.getRGB (x, y - 1)).getBlue ( );
        c2 = new Color (this.Scale_grey_Image.getRGB (x + 1, y)).getBlue ( );


        return Math.abs (c1 - c2);
    }

    private int c_left(int x, int y) {
        int c1;
        int c2;
        c1 = new Color (this.Scale_grey_Image.getRGB (x, y - 1)).getBlue ( );
        c2 = new Color (this.Scale_grey_Image.getRGB (x - 1, y)).getBlue ( );


        return Math.abs (c1 - c2);
    }

    private int c_up(int x, int y) {
        int c1;
        int c2;
        c1 = new Color (this.Scale_grey_Image.getRGB (x + 1, y)).getBlue ( );
        c2 = new Color (this.Scale_grey_Image.getRGB (x - 1, y)).getBlue ( );


        return Math.abs (c1 - c2);
    }


    // Helper method that returns the index of the minimum entry
    private int find_Min_Cost_InCostMatrix( ) {
        int min_X_Idx;
        int height;
        long min_val;
        min_X_Idx = 0;
        height = this.inHeight - 1;
        min_val = costMat[height][0];
        for (int i = this.Width_curr - 1; i >= 1; i--) {
            if (costMat[height][i] >= min_val) {
                continue;
            }
            min_X_Idx = i;
            min_val = costMat[height][i];
        }
        return min_X_Idx;
    }

    private long MinNeighbor_looking(int y, int x) {
        long Up_neighbor;
        long C_up;
        long L_neighbor;
        long R_neighbor;
        long minBetweenUpAndRight;
        Up_neighbor = this.costMat[y - 1][x];
        C_up = 0;
        L_neighbor = Long.MAX_VALUE;
        R_neighbor = Long.MAX_VALUE;

        if (x <= 0 || ((x + 1) >= this.Width_curr)) {
        } else {
            C_up = c_up (x, y);
            Up_neighbor += C_up;
        }
        if (x <= 0) {
        } else {
            int C_left = c_left (x, y);
            L_neighbor = this.costMat[y - 1][x - 1] + (C_up + C_left);
        }
        if (x + 1 >= this.Width_curr) {
        } else {
            int C_right;
            C_right = c_right (x, y);
            R_neighbor = costMat[y - 1][x + 1] + (C_up + C_right);
        }
        minBetweenUpAndRight = Math.min (Up_neighbor, R_neighbor);
        return Math.min (L_neighbor, minBetweenUpAndRight);
    }

    /**This method Initialize matrix with the positions of the pixels in the original image. */
    private void T_IndicesMat_calc( ) {
        this.IndicesMat = new int[this.inHeight][this.inWidth];
        for (int i = this.inHeight - 1; i >= 0; i--) {
            for (int j = 0; j < this.inWidth; j++) {
                // Set to col number
                this.IndicesMat[i][j] = j;
            }
        }
    }

    private BufferedImage BestSeam_del(BufferedImage currentColorImage) {
        int Min_Idx;
        int Width_New;
        int[] best_curr_Seam;
        int[][] New_T_Mat;
        boolean[][] new_Img_Mask;
        BufferedImage New_Img;
        BufferedImage Grey_Img_New;
        Min_Idx = this.find_Min_Cost_InCostMatrix ( );
        best_curr_Seam = BestSeam_Trace_Back (Min_Idx);
        this.SeamsInd_Mat_final[this.Count_S_remove] = best_curr_Seam;
        Width_New = this.Width_curr - 1;
        New_Img = new BufferedImage (Width_New, this.inHeight, this.workingImageType);
        Grey_Img_New = new BufferedImage (Width_New, this.inHeight, this.workingImageType);
        new_Img_Mask = new boolean[this.inHeight][Width_New];
        New_T_Mat = new int[this.inHeight][Width_New];

        for (int y = 0; y < this.inHeight; y++) {
            int currentXIndex;
            currentXIndex = best_curr_Seam[y];
            for (int x = this.Width_curr - 1; x >= 0; x--) {
                if (x >= currentXIndex) {
                    if (x > currentXIndex) {
                        New_Img.setRGB ((x - 1), y, currentColorImage.getRGB (x, y));
                        Grey_Img_New.setRGB ((x - 1), y, this.Scale_grey_Image.getRGB (x, y));
                        new_Img_Mask[y][x - 1] = this.imageMask[y][x];
                        New_T_Mat[y][x - 1] = this.IndicesMat[y][x];
                    }
                } else {
                    New_Img.setRGB (x, y, currentColorImage.getRGB (x, y));
                    Grey_Img_New.setRGB (x, y, this.Scale_grey_Image.getRGB (x, y));
                    new_Img_Mask[y][x] = this.imageMask[y][x];
                    New_T_Mat[y][x] = this.IndicesMat[y][x];

                }
            }
        }
        initialization (New_T_Mat, new_Img_Mask, Grey_Img_New);

        return New_Img;
    }

    private void initialization(int[][] new_T_Mat, boolean[][] new_Img_Mask, BufferedImage grey_Img_New) {
        this.Width_curr = this.Width_curr - 1;
        this.Scale_grey_Image = grey_Img_New;
        this.imageMask = new_Img_Mask;
        this.Count_S_remove++;
        this.IndicesMat = new_T_Mat;
    }


    //trace back in the cost matrix to find the Best Seam.
    //and update the seams variables.
    private int[] BestSeam_Trace_Back(int startXIndex) {
        int[] b_Seam_Idx;
        int[] T_Seam_Idx;
        int Z;
        b_Seam_Idx = new int[this.inHeight];
        T_Seam_Idx = new int[this.inHeight];
        b_Seam_Idx[this.inHeight - 1] = startXIndex;
        Z = startXIndex;

        for (int y = inHeight - 1; y > 0; y--) {
            long c_up;
            long c_left;
            long neighbourLeft;
            c_up = 0;
            neighbourLeft = -1;

            if (Z > 0 && ((Z + 1) < this.Width_curr)) {
                c_up = c_up (Z, y);
            }
            if (Z > 0) {
                c_left = c_left (Z, y);
                neighbourLeft = this.costMat[y - 1][Z - 1] + c_up + c_left;
            }

            if (this.costMat[y][Z] != (this.energyMat[y][Z] + this.costMat[y - 1][Z] + c_up)) {
                if (this.costMat[y][Z] == (this.energyMat[y][Z] + neighbourLeft)) {
                    Z = Z - 1;
                } else {
                    Z = Z + 1;
                }
            }

            b_Seam_Idx[y - 1] = Z;
            T_Seam_Idx[y - 1] = IndicesMat[y][Z];
        }
        this.SeamsIndices_final.add (T_Seam_Idx);
        return b_Seam_Idx;
    }

    private void accept(Integer y, Integer x) {
        long min_V = 0;
        if (y > 0) {
            min_V = MinNeighbor_looking (y, x);
        }
        this.costMat[y][x] = this.energyMat[y][x] + min_V;
    }

    private void accept2(Integer y, Integer x) {
        int c1, c2;
        long e1, e2, e3;
        c1 = new Color (this.Scale_grey_Image.getRGB (x, y)).getBlue ( );
        c2 = x >= (this.Width_curr - 1) ? new Color (this.Scale_grey_Image.getRGB (x - 1, y)).getBlue ( )
                : new Color (this.Scale_grey_Image.getRGB (x + 1, y)).getBlue ( );
        e1 = Math.abs (c1 - c2);

        c2 = y >= (this.inHeight - 1) ? new Color (this.Scale_grey_Image.getRGB (x, y - 1)).getBlue ( )
                : new Color (this.Scale_grey_Image.getRGB (x, y + 1)).getBlue ( );
        e2 = Math.abs (c1 - c2);

        e3 = !this.imageMask[y][x] ? 0 : Integer.MIN_VALUE;

        this.energyMat[y][x] = e1 + e2 + e3;
    }


    // MARK: An inner interface for functional programming.
    @FunctionalInterface
    interface ResizeOperation {
        BufferedImage resize();
    }
}