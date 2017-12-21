package de.pbc.stata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.stata.sfi.Macro;
import com.stata.sfi.Matrix;

/**
 * Collection of non-trivial utility methods for Stata's Functional Interface
 * (SFI).
 * 
 * @author Philipp B. Cornelius
 * @version 1 (2015-07-06)
 */
public class StataUtils {
	
	// PUBLIC -------------------------------------------------------- //
	
	/**
	 * Returns a matrix as a 2-dimensional array. The first dimension is the
	 * rows and the second dimension is the columns.
	 * 
	 * @param name a valid matrix name
	 * @return a 2-dimensional array
	 */
	public static double[][] getMatrix(String name) {
		Objects.requireNonNull(name);
		double[] temp = Matrix.getMatrix(name);
		
		if (temp != null) {
			int rows = Matrix.getMatrixRow(name);
			int cols = Matrix.getMatrixCol(name);
			
			double[][] m = new double[rows][cols];
			for (int i = 0; i < temp.length; i++) {
				m[i / cols][i % cols] = temp[i];
			}
			
			return m;
		}
		
		return null;
	}
	
	/**
	 * Copies and transposes the given matrix.
	 * 
	 * @param m a matrix
	 * @return a transposed copy
	 */
	public static double[][] transposeMatrix(double[][] m) {
		Objects.requireNonNull(m);
		double[][] temp = new double[m[0].length][m.length];
		for (int i = 0; i < m.length; i++)
			for (int j = 0; j < m[0].length; j++)
				temp[j][i] = m[i][j];
		return temp;
	}
	
	public static String[] getMacroArray(String name) {
		Objects.requireNonNull(name);
		return Macro.getLocal(name).split(" ");
	}
	
	public static List<String> getMacroList(String name) {
		return Arrays.asList(getMacroArray(name));
	}
	
	public static String correctRounding(Double val, int scale) {
		BigDecimal valBD = BigDecimal.valueOf(val);
		BigDecimal valBDRounded = valBD.setScale(scale, RoundingMode.HALF_UP);
		
		if (valBD.signum() == -1 && valBDRounded.signum() != -1) {
			return "-" + valBDRounded;
		} else {
			return valBDRounded.toString();
		}
	}
	
}