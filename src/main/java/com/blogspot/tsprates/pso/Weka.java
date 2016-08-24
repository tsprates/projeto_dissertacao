package com.blogspot.tsprates.pso;

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

    private final List<List<String>> kpastas;

    private final int K;

    private final String tabela;

    private final String colId;

    private final String colSaida;

    private final String optsJ48, optsSMO, optsRBF;

    private int numClasses;

    private double[][] efet = null; // efetividade

    private double[][] acur = null; // efetividade

    /**
     * Construtor.
     *
     * @param config
     * @param K
     * @param kpastas
     */
    public Weka(Properties config, final int K, List<List<String>> kpastas)
    {
        this.kpastas = kpastas;
        this.K = K;

        this.tabela = config.getProperty("tabela");
        this.colSaida = config.getProperty("saida");
        this.colId = config.getProperty("id");

        this.optsJ48 = String.format("-C %s",
                config.getProperty("J48.confidence_factor"));

        this.optsSMO = String.format("-C %s -K %s -E %s",
                config.getProperty("SMO.complexity_parameter_c"),
                config.getProperty("SMO.kernel_function"),
                config.getProperty("SMO.function_exponent"));

        this.optsRBF = String.format("-B %s -W %s",
                config.getProperty("RBF.clusters"),
                config.getProperty("RBF.min_std_dev_clusters"));

        classificar();
    }

    /**
     * Classifica algoritmos do WEKA por classes da classificação.
     */
    private void classificar()
    {

        try
        {
            InstanceQuery query = new InstanceQuery();
            query.setUsername(DB.USERNAME);
            query.setPassword(DB.PASSWORD);

            for (int i = 0; i < K; i++)
            {
                String ids = StringUtils.join(kpastas.get(i), ", ");

                // Treinamento
                final String sqlTrain = "SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " NOT IN (" + ids + ")"
                        + "ORDER BY " + colSaida + " ASC";
                query.setQuery(sqlTrain);

                Instances train = query.retrieveInstances();
                train.setClassIndex(train.attribute(colSaida).index());

                Instances trainData = removerColId(train);

                // Árvore de Decisão
                J48 j48 = new J48();
                j48.setOptions(Utils.splitOptions(optsJ48));
                j48.buildClassifier(trainData);

                // SVM
                SMO smo = new SMO();
                smo.setOptions(Utils.splitOptions(optsSMO));
                smo.buildClassifier(trainData);

                // Rede Neural de Base Radial
                RBFNetwork rbf = new RBFNetwork();
                rbf.setOptions(Utils.splitOptions(optsRBF));
                rbf.buildClassifier(trainData);

                // Validação
                final String sqlTest = "SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " IN (" + ids + ")"
                        + "ORDER BY " + colSaida + " ASC";
                query.setQuery(sqlTest);

                Instances test = query.retrieveInstances();
                test.setClassIndex(test.attribute(colSaida).index());

//                System.out.println("Índices saídas:\n");
//                Attribute attr = test.attribute(train.attribute(colSaida).index());
//                for (int j = 0; j < attr.numValues(); j++)
//                {
//                    System.out.println(j + " : " + attr.value(j));
//                }
//                System.out.println();
                
                // Remove atributo id
                Instances testData = removerColId(test);

                // Validação
                Evaluation evalJ48 = new Evaluation(trainData);
                evalJ48.evaluateModel(j48, testData);

                Evaluation evalSMO = new Evaluation(trainData);
                evalSMO.evaluateModel(smo, testData);

                Evaluation evalRBF = new Evaluation(trainData);
                evalRBF.evaluateModel(rbf, testData);

                // total de classes 
                this.numClasses = trainData.numClasses();

                if (efet == null)
                {
                    efet = new double[3][numClasses];
                }

                if (acur == null)
                {
                    acur = new double[3][numClasses];
                }

                for (int j = 0; j < numClasses; j++)
                {
                    efet[0][j] += evalJ48.precision(j) * evalJ48.recall(j);
                    efet[1][j] += evalSMO.precision(j) * evalSMO.recall(j);
                    efet[2][j] += evalRBF.precision(j) * evalRBF.recall(j);

                    acur[0][j] += (evalJ48.numTruePositives(j) + evalJ48.numTrueNegatives(j)) / (evalJ48.numTruePositives(j) + evalJ48.numTrueNegatives(j) + evalJ48.numFalsePositives(j) + evalJ48.numFalseNegatives(j));
                    acur[1][j] += (evalSMO.numTruePositives(j) + evalSMO.numTrueNegatives(j)) / (evalSMO.numTruePositives(j) + evalSMO.numTrueNegatives(j) + evalSMO.numFalsePositives(j) + evalSMO.numFalseNegatives(j));
                    acur[2][j] += (evalRBF.numTruePositives(j) + evalRBF.numTrueNegatives(j)) / (evalRBF.numTruePositives(j) + evalRBF.numTrueNegatives(j) + evalRBF.numFalsePositives(j) + evalRBF.numFalseNegatives(j));

                }
            }

            for (int i = 0; i < numClasses; i++)
            {
                efet[0][i] /= K;
                efet[1][i] /= K;
                efet[2][i] /= K;

                acur[0][i] /= K;
                acur[1][i] /= K;
                acur[2][i] /= K;
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Remove atributo ID da tabela (SQL) e retorna o objeto Intances do WEKA.
     *
     * @param instance
     * @return
     * @throws Exception
     */
    private Instances removerColId(Instances instance) throws Exception
    {
        int colIdIndex = instance.attribute(colId).index() + 1;

        Remove remAtr = new Remove();
        remAtr.setOptions(Utils.splitOptions("-R " + colIdIndex));
        remAtr.setInputFormat(instance);

        return Filter.useFilter(instance, remAtr);
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
