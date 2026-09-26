package com.atlasiq.intelligence;
import com.atlasiq.qir.*;
import java.util.List;
public record ArchitectureDiff(
 String fromAnalysisId,String toAnalysisId,
 List<QirNode> addedNodes,List<QirNode> removedNodes,List<NodeChange> changedNodes,
 List<QirEdge> addedEdges,List<QirEdge> removedEdges,
 List<Finding> addedFindings,List<Finding> resolvedFindings) {
 public record NodeChange(QirNode before,QirNode after){}
 public int changeCount(){return addedNodes.size()+removedNodes.size()+changedNodes.size()+addedEdges.size()+removedEdges.size()+addedFindings.size()+resolvedFindings.size();}
}
