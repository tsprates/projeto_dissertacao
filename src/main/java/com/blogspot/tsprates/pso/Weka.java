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

    public double[] getEfetividadeArray()
    {
        double[] total = new double[]
        {
            0.0, 0.0, 0.0
        };

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
                        + "WHERE " + colId + " NOT IN (" + ids + ")");

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
                        + "WHERE " + colId + " IN (" + ids + ")");

                Instances test = query.retrieveInstances();
                test.setClassIndex(test.attribute(colSaida).index());

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

                // Calcula efetividade global
                int numClasses = trainData.numClasses();
                double totalJ48 = 0.0;
                double totalSMO = 0.0;
                double totalRBF = 0.0;

                for (int j = 0; j < numClasses; j++)
                {
                    totalJ48 += evalJ48.precision(j) * evalJ48.recall(j);
                    totalSMO += evalSMO.precision(j) * evalSMO.recall(j);
                    totalRBF += evalRBF.precision(j) * evalRBF.recall(j);
                }

                total[0] += totalJ48 / numClasses;
                total[1] += totalSMO / numClasses;
                total[2] += totalRBF / numClasses;
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        total[0] = total[0] / K;
        total[1] = total[1] / K;
        total[2] = total[2] / K;

        return total;
    }

    private Remove criarRemove(Instances instance) throws Exception
    {
        int colIdIndex = instance.attribute(colId).index() + 1;

        Remove remAtr = new Remove();
        remAtr.setOptions(Utils.splitOptions("-R " + colIdIndex));
        remAtr.setInputFormat(instance);

        return remAtr;
    }
}
