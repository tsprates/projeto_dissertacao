package com.github.tsprates.pso;

import org.apache.commons.lang3.StringUtils;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.RBFNetwork;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.Utils;
import weka.experiment.InstanceQuery;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.List;
import java.util.Properties;

/**
 * WEKA.
 *
 * @author thiago
 */
public class Weka
{

    private final String tabela;

    private final String colId;

    private final String colClasse;

    private final String optsJ48, optsSMO, optsRBF;

    private int numClasses;

    private double[][] efet = null; // efetividade

    private double[][] acur = null; // acurácia

    /**
     * Construtor.
     *
     * @param config
     */
    public Weka( Properties config )
    {
        this.tabela = config.getProperty( "tabela" );
        this.colClasse = config.getProperty( "saida" );
        this.colId = config.getProperty( "id" );

        this.optsJ48 = String.format( "-C %s", config.getProperty( "J48.confidence_factor" ) );

        this.optsSMO =
                        String.format( "-C %s -K %s -E %s", config.getProperty( "SMO.complexity_parameter_c" ),
                                       config.getProperty( "SMO.kernel_function" ),
                                       config.getProperty( "SMO.function_exponent" ) );

        this.optsRBF = String.format( "-B %s -W %s", config.getProperty( "RBF.clusters" ),
                                      config.getProperty( "RBF.min_std_dev_clusters" ) );
    }

    /**
     * Classifica algoritmos do WEKA por classes.
     *
     * @param K
     * @param kpastas
     */
    public void classificar( int K, List<List<String>> kpastas )
    {
        // reset
        efet = null;
        acur = null;

        try
        {
            InstanceQuery query = new InstanceQuery();
            query.setUsername( DB.USERNAME );
            query.setPassword( DB.PASSWORD );

            for ( int i = 0; i < K; i++ )
            {
                String ids = StringUtils.join( kpastas.get( i ), ", " );

                // Treinamento
                final String sqlTrain = "SELECT * " + "FROM " + tabela + " " + "WHERE " + colId + " NOT IN (" + ids
                                + ")" + "ORDER BY " + colClasse + " ASC";
                query.setQuery( sqlTrain );

                Instances train = query.retrieveInstances();
                train.setClassIndex( train.attribute( colClasse ).index() );

                Instances trainData = removerColId( train );

                // Árvore de Decisão
                J48 j48 = new J48();
                j48.setOptions( Utils.splitOptions( optsJ48 ) );
                j48.buildClassifier( trainData );

                // SVM
                SMO smo = new SMO();
                smo.setOptions( Utils.splitOptions( optsSMO ) );
                smo.buildClassifier( trainData );

                // Rede Neural de Base Radial
                RBFNetwork rbf = new RBFNetwork();
                rbf.setOptions( Utils.splitOptions( optsRBF ) );
                rbf.buildClassifier( trainData );

                // Teste
                final String sqlTest = "SELECT * " + "FROM " + tabela + " " + "WHERE " + colId + " IN (" + ids + ")"
                                + "ORDER BY " + colClasse + " ASC";
                query.setQuery( sqlTest );

                Instances test = query.retrieveInstances();
                test.setClassIndex( test.attribute( colClasse ).index() );

                // Remove atributo id
                Instances testData = removerColId( test );

                Evaluation evalJ48 = new Evaluation( trainData );
                evalJ48.evaluateModel( j48, testData );

                Evaluation evalSMO = new Evaluation( trainData );
                evalSMO.evaluateModel( smo, testData );

                Evaluation evalRBF = new Evaluation( trainData );
                evalRBF.evaluateModel( rbf, testData );

                // total de classes
                numClasses = trainData.numClasses();

                if ( efet == null )
                {
                    efet = new double[3][numClasses];
                }

                if ( acur == null )
                {
                    acur = new double[3][numClasses];
                }

                for ( int j = 0; j < numClasses; j++ )
                {
                    efet[0][j] += ( evalJ48.numTruePositives( j )
                                    / ( evalJ48.numTruePositives( j ) + evalJ48.numFalseNegatives( j ) ) )
                                    * ( evalJ48.numTrueNegatives( j )
                                    / ( evalJ48.numTrueNegatives( j ) + evalJ48.numFalsePositives( j ) ) );
                    efet[1][j] += ( evalSMO.numTruePositives( j )
                                    / ( evalSMO.numTruePositives( j ) + evalSMO.numFalseNegatives( j ) ) )
                                    * ( evalSMO.numTrueNegatives( j )
                                    / ( evalSMO.numTrueNegatives( j ) + evalSMO.numFalsePositives( j ) ) );
                    efet[2][j] += ( evalRBF.numTruePositives( j )
                                    / ( evalRBF.numTruePositives( j ) + evalRBF.numFalseNegatives( j ) ) )
                                    * ( evalRBF.numTrueNegatives( j )
                                    / ( evalRBF.numTrueNegatives( j ) + evalRBF.numFalsePositives( j ) ) );

                    acur[0][j] += ( evalJ48.numTruePositives( j ) + evalJ48.numTrueNegatives( j ) )
                                    / ( evalJ48.numTruePositives( j ) + evalJ48.numTrueNegatives( j )
                                    + evalJ48.numFalsePositives( j ) + evalJ48.numFalseNegatives( j ) );
                    acur[1][j] += ( evalSMO.numTruePositives( j ) + evalSMO.numTrueNegatives( j ) )
                                    / ( evalSMO.numTruePositives( j ) + evalSMO.numTrueNegatives( j )
                                    + evalSMO.numFalsePositives( j ) + evalSMO.numFalseNegatives( j ) );
                    acur[2][j] += ( evalRBF.numTruePositives( j ) + evalRBF.numTrueNegatives( j ) )
                                    / ( evalRBF.numTruePositives( j ) + evalRBF.numTrueNegatives( j )
                                    + evalRBF.numFalsePositives( j ) + evalRBF.numFalseNegatives( j ) );
                }
            }

            for ( int i = 0; i < numClasses; i++ )
            {
                efet[0][i] /= K;
                efet[1][i] /= K;
                efet[2][i] /= K;

                acur[0][i] /= K;
                acur[1][i] /= K;
                acur[2][i] /= K;
            }
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }

    }

    /**
     * Remove atributo ID da tabela (SQL) e retorna o objeto Intances do WEKA.
     *
     * @param instance
     * @return
     * @throws Exception
     */
    private Instances removerColId( Instances instance )
                    throws Exception
    {
        final int colIdIndex = instance.attribute( colId ).index() + 1;

        // remove o atributo classe
        Remove rem = new Remove();
        rem.setOptions( Utils.splitOptions( "-R " + colIdIndex ) );
        rem.setInvertSelection( false );
        rem.setInputFormat( instance );

        return Filter.useFilter( instance, rem );
    }

    /**
     * Retorna número de classes.
     *
     * @return Número de classes
     */
    public int numClasses()
    {
        return numClasses;
    }

    /**
     * Retorna matriz de algoritmos (J48, SMO e RBF) por classes da acurácia.
     *
     * @return Matriz da acurácia.
     */
    public double[][] acuracia()
    {
        return acur;
    }

    /**
     * Retorna matriz de algoritmos (J48, SMO e RBF) por classes da efetividade.
     *
     * @return Matriz da efetividade.
     */
    public double[][] efetividade()
    {
        return efet;
    }
}
