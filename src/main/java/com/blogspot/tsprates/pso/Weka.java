package com.blogspot.tsprates.pso;

import java.util.List;

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

    public Weka(List<List<String>> kpastas,
            final int K,
            final String tabela,
            final String colId,
            final String colSaida)
    {
        this.kpastas = kpastas;
        this.K = K;
        this.tabela = tabela;
        this.colId = colId;
        this.colSaida = colSaida;
    }

    public double[] getEfetividadeArray()
    {
        double[] total = new double[] { 0.0, 0.0, 0.0 };
            
        try
        {
            InstanceQuery query = new InstanceQuery();
            query.setUsername("postgres");
            query.setPassword("admin");
            
            for (int i = 0; i < K; i++)
            {
                String ids = StringUtils.join(kpastas.get(i), ", ");

                // validação
                query.setQuery("SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " NOT IN (" + ids + ")");

                Instances train = query.retrieveInstances();
                train.setClassIndex(train.attribute(colSaida).index());

                Remove removeAtribTrain = criarRemove(train);
                Instances trainData = Filter.useFilter(train, removeAtribTrain);
                
//                System.out.println(new Instances(trainData, 0));

                // criar classificadores
                J48 j48 = new J48();
                j48.buildClassifier(trainData);

                SMO smo = new SMO();
                smo.buildClassifier(trainData);
                
                RBFNetwork rbf = new RBFNetwork();
                rbf.buildClassifier(trainData);
                
                // teste
                query.setQuery("SELECT * "
                        + "FROM " + tabela + " "
                        + "WHERE " + colId + " IN (" + ids + ")");

                Instances test = query.retrieveInstances();
                test.setClassIndex(test.attribute(colSaida).index());

                Remove removeAtribTest = criarRemove(test);
                Instances testData = Filter.useFilter(test, removeAtribTest);

                Evaluation evalJ48 = new Evaluation(trainData);
                evalJ48.evaluateModel(j48, testData);
                
                Evaluation evalSMO = new Evaluation(trainData);
                evalSMO.evaluateModel(smo, testData);
                
                Evaluation evalRBF = new Evaluation(trainData);
                evalRBF.evaluateModel(rbf, testData);

                // efetividade global
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
        Remove removeAtrib = new Remove();
        removeAtrib.setOptions(Utils.splitOptions("-R " + colIdIndex));
        removeAtrib.setInputFormat(instance);
        return removeAtrib;
    }
}
