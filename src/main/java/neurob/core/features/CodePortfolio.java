package neurob.core.features;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

@Deprecated
public class CodePortfolio {
	private int size;
	private BufferedImage image;
	private int featureSize;
	
	/**
	 * <p>Creates a CodePortfolio with the dimension of the given size squared
	 * </p><p>
	 * {@code new CodePortfolio(64); // Creates a 64*64 pixel image}
	 * </p> 
	 * @param dimensionSize
	 */
	public CodePortfolio(int dimensionSize, String code) {
		size = dimensionSize;
		featureSize = size*size;
		
		setData(code);
	}
	
	public int getFeatureCount(){return featureSize;}
	
	/**
	 * Generates internally an image of the code, that serves as input for a neural net
	 * @param code
	 */
	private void setData(String code){
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
		int rootCodeLength = (int) Math.floor(Math.sqrt(code.length()));
		BufferedImage img = new BufferedImage(rootCodeLength, rootCodeLength, BufferedImage.TYPE_BYTE_GRAY);
		WritableRaster raster = img.getRaster();
		
		// fill image
		for(int y = 0; y < rootCodeLength; y++){
			for(int x = 0; x < rootCodeLength; x++){
				int px = code.charAt(x+y*rootCodeLength);
				raster.setPixel(x, y, new int[]{x});
			}
		}
		
		// resize image
		Image tmp = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
		BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_GRAY);
		
		Graphics2D g2d = scaled.createGraphics();
		g2d.drawImage(tmp,0,0,null);
		g2d.dispose();
		
		image = scaled;
		
	}
	
	public String toString(){
		// return a string representation
		ArrayList<String> values = new ArrayList<String>();
		
		WritableRaster raster = image.getRaster();
		for(int y=0; y<size; y++){
			for(int x = 0; x<size; x++){
				int px[] = raster.getPixel(x, y, new int[]{0});
				values.add(String.valueOf(px[0]));
			}
		}
		
		return String.join(",", values);
	}
	
	public BufferedImage getImage(){
		return image;
	}
	
	public void display(){
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 300);
		
		frame.getContentPane().add(new JLabel(new ImageIcon(image)));
		
		frame.setVisible(true);
		
	}
}
