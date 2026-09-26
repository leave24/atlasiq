package com.atlasiq.intelligence;
import java.util.List;
public record PrChangeImpact(String baselineAnalysisId,String candidateAnalysisId,String riskLevel,int score,List<String> reasons,ArchitectureDiff diff) {}
