package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	// MARK: fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;

	// MARK: constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights, int outWidth,
			int outHeight) {
		super(); // initializing for each loops...

		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}

	public ImageProcessor(Logger logger, BufferedImage workingImage, RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights, workingImage.getWidth(), workingImage.getHeight());
	}

	// Changes the picture's hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;

		BufferedImage ans = newEmptyInputSizedImage();

		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r * c.getRed() / max;
			int green = g * c.getGreen() / max;
			int blue = b * c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});

		logger.log("Changing hue done!");

		return ans;
	}


	// Sets the ForEach parameters with the input dimensions
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}

	// Sets the ForEach parameters with the output dimensions
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}

	// A helper method that creates an empty image with the specified input dimensions.
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}

	// A helper method that creates an empty image with the specified output dimensions.
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}

	// A helper method that creates an empty image with the specified dimensions.
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}

	// A helper method that deep copies the current working image.
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();

		forEach((y, x) -> output.setRGB(x, y, workingImage.getRGB(x, y)));

		return output;
	}
	
	public BufferedImage greyscale() {

		logger.log(" creates a greyscale image");
		BufferedImage output = newEmptyInputSizedImage();
		forEach((Integer y, Integer x) -> {
			Color c;
			Color greyScale;
			int R_Grey;
			int G_Grey;
			int B_Grey;
			int Grey_Avg;
			c = new Color(workingImage.getRGB(x, y));
			R_Grey = (rgbWeights.redWeight) * c.getRed();
			G_Grey = (rgbWeights.greenWeight) * c.getGreen();
			B_Grey = (rgbWeights.blueWeight) * c.getBlue();
			Grey_Avg = (R_Grey + G_Grey + B_Grey ) / (rgbWeights.redWeight + rgbWeights.greenWeight + rgbWeights.blueWeight);
			greyScale = new Color(Grey_Avg, Grey_Avg, Grey_Avg);
			output.setRGB(x, y, greyScale.getRGB());
		});
		logger.log("greyscaling done!");
		return output;

	}

	public BufferedImage nearestNeighbor() {

		logger.log("applies nearest neighbor interpolation");
		BufferedImage ans;
		float Rat_X;
		float Rat_Y;
		ans = this.newEmptyOutputSizedImage( );
		Rat_X = (float)this.inWidth / (float)this.outWidth;
		Rat_Y = (float)this.inHeight / (float)this.outHeight;

		this.setForEachOutputParameters( );
		this.forEach((Integer y, Integer x) -> {
			int near_X;
			int near_Y;
			near_X = Math.round((float)x * Rat_X);
			near_Y = Math.round((float)y * Rat_Y);
			near_X = (near_X > this.inWidth -1) ? this.inWidth -1 : near_X;
			near_Y = (near_Y > this.inHeight -1) ? this.inHeight -1 : near_Y;
			ans.setRGB(x, y, this.workingImage.getRGB(near_X, near_Y));
		});
		logger.log("nearest neighbor interpolation applied");
		return ans;

	}

}
