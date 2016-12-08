package neurob.core.features;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import neurob.core.features.interfaces.ConvolutionFeatures;
import neurob.exceptions.NeuroBException;

public class CodePortfolios implements ConvolutionFeatures {
	private int dim;
	private int pixels; // has to be dim*dim
	// Features
	private ArrayList<BufferedImage> features;
	/**
	 * The image size generated will be of size {@code dimension * dimension}
	 * @param dimension 
	 */
	public CodePortfolios(int dimension) {
		this.dim = dimension;
		pixels = dim*dim;
		features = new ArrayList<BufferedImage>();
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
		
		// resize image
		Image tmp = img.getScaledInstance(dim, dim, Image.SCALE_SMOOTH);
		BufferedImage scaled = new BufferedImage(dim, dim, BufferedImage.TYPE_BYTE_GRAY);
		
		Graphics2D g2d = scaled.createGraphics();
		g2d.drawImage(tmp,0,0,null);
		g2d.dispose();
		
		// return image
		return scaled;
	}
	
	private double[] translateImageFeatureToArray(BufferedImage image){
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
	
	private String translateImageFeatureToString(BufferedImage image){
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
	public void addData(String code) throws NeuroBException {
		features.add(generateFeatureImage(code));
	}

	@Override
	public List<String> getFeatureStrings() {
		ArrayList<String> strfeatures = new ArrayList<String>();
		
		for(BufferedImage img : getFeatureImages()){
			strfeatures.add(translateImageFeatureToString(img));
		}
		
		return strfeatures;
	}

	@Override
	public int getfeatureDimension() {
		return pixels;
	}

	@Override
	public void reset() {
		features.clear();
	}

	@Override
	public List<BufferedImage> getFeatureImages() {
		return features;
	}

}
