/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2014/12/9 16:56</create-date>
 *
 * <copyright file="Index.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.model;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.model.crf.CRFModel;
import com.hankcs.hanlp.model.crf.Table;

import java.util.LinkedList;

import static com.hankcs.hanlp.utility.Predefine.logger;

/**
 * 静态CRF分词模型
 * @author hankcs
 */
public class CRFSegmentModel extends CRFModel
{
    public static CRFModel crfModel;
    static
    {
        logger.info("CRF分词模型正在加载 " + HanLP.Config.CRFSegmentModelPath);
        long start = System.currentTimeMillis();
        crfModel = CRFModel.loadTxt(HanLP.Config.CRFSegmentModelPath, new CRFSegmentModel());
        if (crfModel == null)
        {
            logger.severe("CRF分词模型加载 " + HanLP.Config.CRFSegmentModelPath + " 失败，耗时 " + (System.currentTimeMillis() - start) + " ms");
            System.exit(-1);
        }
        else
            logger.info("CRF分词模型加载 " + HanLP.Config.CRFSegmentModelPath + " 成功，耗时 " + (System.currentTimeMillis() - start) + " ms");
    }

    private static int idB = crfModel.getTagId("B");
    private static int idS = crfModel.getTagId("S");

    @Override
    public void tag(Table table)
    {
        int size = table.size();
        if (size == 1)
        {
            table.setLast(0, "S");
            return;
        }
        double bestScore;
        int bestTag;    // BESM
        int tagSize = id2tag.length;
        LinkedList<double[]> scoreList = computeScoreList(table, 0);    // 0位置命中的特征函数
        // 0位置只可能是B或者S
        {
            bestScore = computeScore(scoreList, idB);
            bestTag = idB;
            double curScore = computeScore(scoreList, idS);
            if (curScore > bestScore)
            {
                bestTag = idS;
            }
        }
        table.setLast(0, id2tag[bestTag]);
        int preTag = bestTag;
        // 0位置打分完毕，接下来打剩下的
        for (int i = 1; i < size - 1; ++i)
        {
            scoreList = computeScoreList(table, i);    // i位置命中的特征函数
            bestScore = -1000.0;
            for (int j = 0; j < tagSize; ++j)   // i位置的标签遍历
            {
                double curScore = computeScore(scoreList, j);
                if (matrix != null)
                {
                    curScore += matrix[preTag][j];
                }
                if (curScore > bestScore)
                {
                    bestScore = curScore;
                    bestTag = j;
                }
            }
            table.setLast(i, id2tag[bestTag]);
            preTag = bestTag;
        }
        // size - 1位置只可能是E或者S，其实从最终合并逻辑上看，S就足够
        table.setLast(size - 1, "S");
    }
}
