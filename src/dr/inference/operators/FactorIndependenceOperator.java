package dr.inference.operators;

import dr.evomodel.continuous.LatentFactorModel;
import dr.inference.model.DiagonalMatrix;
import dr.inference.model.MatrixParameter;
import dr.inference.model.Parameter;
import dr.math.MathUtils;
import dr.math.distributions.MultivariateNormalDistribution;
import dr.math.matrixAlgebra.SymmetricMatrix;

/**
 * Created with IntelliJ IDEA.
 * User: max
 * Date: 5/22/14
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class FactorIndependenceOperator extends AbstractCoercableOperator{
    private static final String FACTOR_OPERATOR="factorOperator";
    private LatentFactorModel LFM;
    private MatrixParameter diffusionPrecision;
    double[][] precision;
    double[] mean;
    double[] midMean;
    private int numFactors;
    private boolean randomScan;
    private double scaleFactor;

    public FactorIndependenceOperator(LatentFactorModel LFM, double weight, boolean randomScan, DiagonalMatrix diffusionPrecision, double scaleFactor, CoercionMode mode){
        super(mode);
        this.scaleFactor=scaleFactor;
        this.LFM=LFM;
        setWeight(weight);
        this.randomScan=randomScan;
        this.diffusionPrecision=diffusionPrecision;
        setupParameters();

    }

    private void setupParameters(){
        if(numFactors!=LFM.getFactorDimension()){
            numFactors=LFM.getFactorDimension();
            mean=new double[numFactors];
            midMean=new double[numFactors];
            precision=new double[numFactors][numFactors];}
    }

    private void getPrecision(double[][] precision){
        MatrixParameter Loadings=LFM.getLoadings();
        MatrixParameter Precision=LFM.getColumnPrecision();
        int outerDim=Loadings.getRowDimension();
        int innerDim=Loadings.getColumnDimension();
        for (int i = 0; i <outerDim ; i++) {
            for (int j = i; j <outerDim ; j++) {
                double sum=0;
                for (int k = j; k <innerDim ; k++) {
                    sum+=Loadings.getParameterValue(i,k)*Loadings.getParameterValue(j,k)*Precision.getParameterValue(k,k);
                }
                if(i==j){
                    precision[i][j]=sum+diffusionPrecision.getParameterValue(i,j);
                }
                else{
                    precision[i][j]=sum;
                    precision[j][i]=sum;
                }
            }

        }
    }

    private void getMean(int column, double[][] variance, double[]midMean, double[] mean){
        MatrixParameter scaledData=LFM.getScaledData();
        MatrixParameter Precision=LFM.getColumnPrecision();
        MatrixParameter Loadings=LFM.getLoadings();
        for (int i = 0; i < Loadings.getRowDimension(); i++) {
            double sum=0;
            for (int j = i; j < Loadings.getColumnDimension(); j++) {
                sum+=Loadings.getParameterValue(i,j)*Precision.getParameterValue(j,j)*scaledData.getParameterValue(j,column);
            }
            midMean[i]=sum;
        }
        for (int i = 0; i <numFactors ; i++) {
            double sum=0;
            for (int j = 0; j < numFactors; j++) {
                sum+=variance[i][j]*midMean[j];
            }
            mean[i]=sum;
        }

//        try {
//            answer=getPrecision().inverse().product(new Matrix(LFM.getLoadings().getParameterAsMatrix())).product(new Matrix(LFM.getColumnPrecision().getParameterAsMatrix())).product(data);
//        } catch (IllegalDimension illegalDimension) {
//            illegalDimension.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
    }

    private void copy(double[] put, int i){
        Parameter working=LFM.getFactors().getParameter(i);
        for (int j = 0; j < working.getSize(); j++) {
            working.setParameterValueQuietly(j, put[j]);
        }
        working.fireParameterChangedEvent();
    }

    public int getStepCount() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPerformanceSuggestion() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getOperatorName() {
        return FACTOR_OPERATOR;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void randomDraw(int i, double[][] variance){
        double[] nextValue;
        getMean(i, variance, midMean, mean);

        nextValue=MultivariateNormalDistribution.nextMultivariateNormalVariance(mean, variance, scaleFactor);

        copy(nextValue, i);
    }

    @Override
    public double doOperation() throws OperatorFailedException {
        setupParameters();
        getPrecision(precision);
        double[][] variance=(new SymmetricMatrix(precision)).inverse().toComponents();
        if(randomScan){
            int i= MathUtils.nextInt(LFM.getFactors().getColumnDimension());
            randomDraw(i, variance);
        }
        for (int i = 0; i <LFM.getFactors().getColumnDimension() ; i++) {
            randomDraw(i, variance);
        }
        LFM.getFactors().fireParameterChangedEvent();

        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }


    @Override
    public double getCoercableParameter() {
        return Math.log(scaleFactor);
    }

    @Override
    public void setCoercableParameter(double value) {
        scaleFactor = Math.exp(value);
    }

    @Override
    public double getRawParameter() {
        return scaleFactor;
    }
}
