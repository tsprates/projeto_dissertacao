package com.blogspot.tsprates.pso;

import java.util.List;
import java.util.Properties;

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

public class Weka
{

    private List<List<String>> kpastas;

    private int K;

    private final String tabela;

    private final String colId;

    private final String colSaida;

    private final String optsJ48, optsSMO, optsRBF;

    private int numClasses;

    /**
     * Construtor.
     *
     * @param kpastas
     * @param K
     * @param config
     */
    public Weka(List<List<String>> kpastas,
            final int K,
            Properties config)
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
    }

    /**
     * Retorna uma matriz de algoritmos do WEKA por classes da classificação.
     *
     * @return Matriz de resultado.
     */
    public double[][] getEfetividadeArray()
    {
        double[][] efet = null; // efetividade
        
        int numCls = -1;

        try
        {
            InstanceQuery query = new InstanceQuery();
            query.setUsername("postgres");
            query.setPassword("admin");

            for (int i = 0; i < K; i++)
            {
                String ids = StringUtils.join(kpastas.get(i), ", ");

                // treinamento
                query.setQuery("SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " NOT IN (" + ids + ")"
                        + "ORDER BY " + colSaida + " ASC");

                Instances train = query.retrieveInstances();
                train.setClassIndex(train.attribute(colSaida).index());

                Remove remAtrTrain = criarRemove(train);
                Instances trainData = Filter.useFilter(train, remAtrTrain);

                // árvore de decisão
                J48 j48 = new J48();
                j48.setOptions(Utils.splitOptions(optsJ48));
                j48.buildClassifier(trainData);

                // svm
                SMO smo = new SMO();
                smo.setOptions(Utils.splitOptions(optsSMO));
                smo.buildClassifier(trainData);

                // rede neural de base radial
                RBFNetwork rbf = new RBFNetwork();
                rbf.setOptions(Utils.splitOptions(optsRBF));
                rbf.buildClassifier(trainData);

                // Validação
                query.setQuery("SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " IN (" + ids + ")"
                        + "ORDER BY " + colSaida + " ASC");

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
                Remove remAtrTest = criarRemove(test);
                Instances testData = Filter.useFilter(test, remAtrTest);

                // Validação
                Evaluation evalJ48 = new Evaluation(trainData);
                evalJ48.evaluateModel(j48, testData);

                Evaluation evalSMO = new Evaluation(trainData);
                evalSMO.evaluateModel(smo, testData);

                Evaluation evalRBF = new Evaluation(trainData);
                evalRBF.evaluateModel(rbf, testData);

                // total de classes 
                if (numCls == -1)
                {
                    numCls = trainData.numClasses();

                    efet = new double[3][numCls];

                    for (int j = 0; j < numCls; j++)
                    {
                        efet[0][j] = 0.0;
                        efet[1][j] = 0.0;
                        efet[2][j] = 0.0;
                    }

                    this.numClasses = numCls;
                }

                for (int j = 0; j < numCls; j++)
                {
                    efet[0][j] += evalJ48.precision(j) * evalJ48.recall(j);
                    efet[1][j] += evalSMO.precision(j) * evalSMO.recall(j);
                    efet[2][j] += evalRBF.precision(j) * evalRBF.recall(j);
                }
            }

            if (efet != null)
            {
                for (int i = 0; i < numCls; i++)
                {
                    efet[0][i] /= K;
                    efet[1][i] /= K;
                    efet[2][i] /= K;
                }
            }

            return efet;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Remove atributo ID sql.
     *
     * @param instance
     * @return
     * @throws Exception
     */
    private Remove criarRemove(Instances instance) throws Exception
    {
        int colIdIndex = instance.attribute(colId).index() + 1;

        Remove remAtr = new Remove();
        remAtr.setOptions(Utils.splitOptions("-R " + colIdIndex));
        remAtr.setInputFormat(instance);

        return remAtr;
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
}
