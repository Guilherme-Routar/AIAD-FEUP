package COSA_Calculations;

import java.util.Vector;

public class COSA {
	
	public static final double calcNormalDistribution(double x, double u, double stdDev) {
		double leftHand = 1 / (stdDev * Math.sqrt(2 * Math.PI));
		double rightHand = Math.pow(Math.E, ((-1 * Math.pow(x - u, 2)) 
											 / 
											 (2 * Math.pow(stdDev, 2))));
		return leftHand * rightHand;
	}

	public static final double calcMean(Vector<Double> Vec) {
		double sum = 0;
		for (double val : Vec) sum += val;
		return sum / Vec.size();
	}

	public static final double calcEntropy(double stdDeviation) {
		return Math.log(stdDeviation * Math.sqrt(2 * Math.PI * Math.E));
	}

	public static final double calcCV(Vector<Double> Vec) {
		return calcStdDev(Vec) / calcMean(Vec);
	}

	public static final double calcStdDev(Vector<Double> Vec) {
		return Math.sqrt(calcVariance(Vec));
	}

	public static final double calcVariance(Vector<Double> Vec) {
		double mean = calcMean(Vec);
		double sum = 0;
		for (double val : Vec)
			sum += (mean - val) * (mean - val);
		return sum / Vec.size();
	}
}
