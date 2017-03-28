package neurob.core.features;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.exceptions.NeuroBException;

public abstract class CodeImages implements ConvolutionFeatures {
	private int dim;
	private int pixels; // has to be dim*dim
	private Path sourceFile;
	
	/**
	 * The image size generated will be of size {@code dimension * dimension}
	 * @param dimension 
	 */
	public CodeImages(int dimension) {
		this.dim = dimension;
		pixels = dim*dim;
	}
	
	@Override
	public String getDataPathIdentifier() {
		return ConvolutionFeatures.super.getDataPathIdentifier() + dim;
	}
	
	@Override
	public double[] generateFeatureArray(String predicate) throws NeuroBException {
		return translateImageFeatureToArray(generateFeatureImage(predicate));
	}
	
	@Override
	public INDArray generateFeatureNDArray(String predicate) throws NeuroBException {
		return Nd4j.create(translateImageFeatureToArray(generateFeatureImage(predicate)));
	}
	
	@Override
	public String generateFeatureString(String predicate) throws NeuroBException {
		return translateImageFeatureToString(generateFeatureImage(predicate));
	}

	@Override
	public BufferedImage generateFeatureImage(String code) throws NeuroBException {
		/*
		 * The code is of size n^2.
		 * 
		 * Step 1: Create an image of size n*n and map each character of the code to a pixel in the image.
		 * As the characters are a value in 0-255, they induce naturally a colour on the gray scale.
		 * 
		 * Step 2: Rescale the image to {@code size}*{@code size}. This may induce loss of information, 
		 * especially if the scaling goes down, but keeps musters in the image.
		 * 
		 */
		
		// set up image
		int n = (int) Math.floor(Math.sqrt(code.length()));
		BufferedImage img = new BufferedImage(n, n, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = img.getRaster();
		
		// fill image
		for(int y = 0; y < n; y++){
			for(int x = 0; x < n; x++){
				int px = code.charAt(x+y*n);
				raster.setPixel(x, y, new int[]{px});
			}
		}
		
		return scaleImage(img);
	}
	
	/**
	 * Scales the given image to the dimensions defined by 
	 * {@link #CodeImages(int) instance construction}.
	 * @param img
	 * @return
	 */
	protected BufferedImage scaleImage(BufferedImage img){
		// resize image
		Image tmp = img.getScaledInstance(dim, dim, Image.SCALE_SMOOTH);
		BufferedImage scaled = new BufferedImage(dim, dim, BufferedImage.TYPE_BYTE_GRAY);
		
		Graphics2D g2d = scaled.createGraphics();
		g2d.drawImage(tmp,0,0,null);
		g2d.dispose();
		
		// return image
		return scaled;
	}
	
	@Override
	public double[] translateImageFeatureToArray(BufferedImage image){
		// return a double[]
		double[] data = new double[pixels];
		
		WritableRaster raster = image.getRaster();
		for(int y=0; y<dim; y++){
			for(int x = 0; x<dim; x++){
				int px[] = raster.getPixel(x, y, new int[]{0});
				data[x+y*dim] = px[0];
			}
		}
		
		return data;
	}
	
	@Override
	public BufferedImage translateArrayFeatureToImage(double[] features) {
		BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = img.getRaster();
		
		for(int y = 0; y < dim; y++){
			for(int x = 0; x < dim; x++){
				int px = (int)features[x+y*dim];
				raster.setPixel(x, y, new int[]{px});
			}
		}
		
		return scaleImage(img);
	}
	
	@Override
	public BufferedImage translateStringFeatureToImage(String featureString) {
		
		int[] pixels = Arrays.asList(featureString.split(",")).stream().mapToInt(Integer::parseInt).toArray();
						// gotta love streams, heh?
		
		BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = img.getRaster();
		
		for(int y=0; y<dim; y++){
			for(int x=0; x<dim; x++){
				raster.setPixel(x, y, new int[]{pixels[x+y*dim]});
			}
		}
		
		return img;
	}
	
	@Override
	public String translateImageFeatureToString(BufferedImage image){
		// return a string representation
		ArrayList<String> values = new ArrayList<String>();
		
		WritableRaster raster = image.getRaster();
		for(int y=0; y<dim; y++){
			for(int x = 0; x<dim; x++){
				int px[] = raster.getPixel(x, y, new int[]{0});
				values.add(String.valueOf(px[0]));
			}
		}
		
		return String.join(",", values);
	}

	@Override
	public int getFeatureDimension() {
		return pixels;
	}
	
	@Override
	public int getImageHeight() {
		return dim;
	}
	
	@Override
	public int getImageWidth() {
		return dim;
	}
	
	@Override
	public int getFeatureChannels() {
		return 1;
	}

	@Override
	public void setSourceFile(Path sourceFile) throws NeuroBException {
		this.sourceFile = sourceFile;
	}

	@Override
	public Path getSourceFile() {
		return sourceFile;
	}

}
