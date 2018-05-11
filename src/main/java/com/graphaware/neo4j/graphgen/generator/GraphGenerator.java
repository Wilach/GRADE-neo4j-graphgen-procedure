/*
 * Copyright (c) 2013-2016 GraphAware
 *
 * This file is part of the GraphAware Framework.
 *
 * GraphAware Framework is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.graphaware.neo4j.graphgen.generator;

import com.graphaware.neo4j.graphgen.YamlParser;
import com.graphaware.neo4j.graphgen.faker.FakerService;
import com.graphaware.neo4j.graphgen.graph.Property;
import com.graphaware.neo4j.graphgen.util.CountSyntaxUtil;
import com.graphaware.neo4j.graphgen.util.LabelsUtil;
import com.graphaware.neo4j.graphgen.util.ShuffleUtil;
import generate.result.GraphResult;
import org.neo4j.graphdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;

public class GraphGenerator {

    private final GraphDatabaseService database;

    private final YamlParser parser;

    private final FakerService fakerService;

    private final Random random;

    public GraphGenerator(GraphDatabaseService database, FakerService fakerService) {
        this.database = database;
        this.parser = new YamlParser();
        this.fakerService = fakerService;
        this.random = fakerService.getRandom();
    }

    public List<Object> generateValues(String name, List<Object> parameters, Long amount) {
        Property property = new Property(name, name, parameters);
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < amount; ++i) {
            values.add(fakerService.getValue(property));
        }

        return values;
    }

    public List<Node> generateNodes(Label[] labels, String propertiesString, long number) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < number; ++i) {
            Node node = database.createNode(labels);
            for (Property property : getProperties(propertiesString)) {
                node.setProperty(property.key(), fakerService.getValue(property));
            }
            nodes.add(node);
        }

        return nodes;
    }
    
    public List<Relationship> oldGenerateRelationships(List<Node> from, List<Node> to, String relationshipType, String properties, String fromCount, String toCount) {
        List<Relationship> list = new ArrayList<>();
        if (from.isEmpty() || to.isEmpty()) {
            return list;
        }

        List<Property> propertyList = getProperties(properties);
        int fromS = CountSyntaxUtil.getCount(fromCount, random);

        List<Integer> fromNodes = ShuffleUtil.shuffle(from, fromS);
        for (int i : fromNodes) {
            int toS = CountSyntaxUtil.getCount(toCount, random);
            List<Integer> toNodes = ShuffleUtil.shuffle(to, toS);
            Node start = from.get(i);
            for (int e : toNodes) {
                Node end = to.get(e);
                Relationship r = start.createRelationshipTo(end, RelationshipType.withName(relationshipType));
                addRelationshipProperties(r, propertyList);
                list.add(r);
            }
        }

        return list;
    }

    public List<Relationship> generateRelationships(String fileName) {
        List<Relationship> list = new ArrayList<>();
        List<Property> propertyList;
        
        
        Label label[] = null;
        
        try {
			OPCPackage pkg = OPCPackage.open("Decision_Cases\\" + fileName);
			XSSFWorkbook wb = new XSSFWorkbook(pkg);
		    XSSFSheet sheet = wb.getSheetAt(0);
		    XSSFRow row;
		    XSSFCell cell;
		
		    int MY_MINIMUM_COLUMN_COUNT = 2;
		    
			// Decide which rows to process
		    int rowStart = 1;
		    int rowEnd = Math.max(1400, sheet.getLastRowNum());

		    for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
		       row = sheet.getRow(rowNum);
		       if (row == null) {
		          // This whole row is empty
		          // Handle it as needed
		          continue;
		       }
		       
		       String relationshipType = "", properties = "{", nodeLabelFrom = "", nodeLabelTo = "", cellString;
		       Node fromNode = null, toNode = null;

		       int lastColumn = Math.max(row.getLastCellNum(), MY_MINIMUM_COLUMN_COUNT);

		       for (int cn = 0; cn < lastColumn; cn++) {
		          cell = row.getCell(cn, XSSFRow.RETURN_BLANK_AS_NULL);
		          if (cell == null) {
		             // The spreadsheet is empty in this cell
		        	 
		          } else {
		             // Do something useful with the cell's contents
                	cellString = cell.toString();
                	if(cn == 4) {
                		relationshipType = cellString;
                	}
                	else if(cn == 0){
                		nodeLabelFrom = cellString;
                		
                	}
                	else if(cn == 1) {
                		fromNode = database.findNode(Label.label(nodeLabelFrom), "name", cellString);
                		
                	}
                	else if(cn == 2) {
                		nodeLabelTo = cellString;
                		
                	}
                	else if(cn == 3) {
                		toNode = database.findNode(Label.label(nodeLabelTo), "name", cellString);
                	}
                	else if(cn > 4) {
                		properties += cellString + "," + " ";
                	}
                	
		          }
		       }
		       if(properties.length() > 1) {
			       if(properties.contains(",")) {
			    	   properties = properties.substring(0, properties.length() - 2);
			       }
			       
			       properties += "}";
		       }
		       else {
		    	   properties = "";
		       }
		       
		       propertyList = getProperties(properties);
		        
		       Relationship r = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType));
		       addRelationshipProperties(r, propertyList);
		       list.add(r);
		    }
		    
		    pkg.close();
		    wb.close();
    	}catch (Exception ioe) {
    		ioe.printStackTrace();
    	}

        return list;
    }

    public GraphResult generateLinkedList(List<Node> nodes, String relationshipType) {
        List<Relationship> relationships = new ArrayList<>();
        int i = 0;
        while (i < nodes.size() - 1) {
            relationships.add(nodes.get(i).createRelationshipTo(nodes.get(++i), RelationshipType.withName(relationshipType)));
        }

        return new GraphResult(nodes, relationships);
    }
    
    public List<Node> generateNodesFromFile(String fileName){
    	Label testLabel[] = null;
    	String testString;
    	List<Node> nodes = new ArrayList<>();
    	try {
			OPCPackage pkg = OPCPackage.open("Decision_Cases\\" + fileName);
			XSSFWorkbook wb = new XSSFWorkbook(pkg);
		    XSSFSheet sheet = wb.getSheetAt(0);
		    XSSFRow row;
		    XSSFCell cell;
		
		    int MY_MINIMUM_COLUMN_COUNT = 2;
		    
			// Decide which rows to process
		    int rowStart = 1;
		    int rowEnd = Math.max(1400, sheet.getLastRowNum());

		    for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
		       row = sheet.getRow(rowNum);
		       if (row == null) {
		          // This whole row is empty
		          // Handle it as needed
		          continue;
		       }
		       
		       String propertyStrings = "{";
		       Node testNode;

		       int lastColumn = Math.max(row.getLastCellNum(), MY_MINIMUM_COLUMN_COUNT);

		       for (int cn = 0; cn < lastColumn; cn++) {
		          cell = row.getCell(cn, XSSFRow.RETURN_BLANK_AS_NULL);
		          if (cell == null) {
		             // The spreadsheet is empty in this cell
		        	 
		          } else {
		             // Do something useful with the cell's contents
		        	// Your code here
                	testString = cell.toString();
                	if(cn == 0) {
                		testLabel = LabelsUtil.fromInput(testString);
                	}
                	else {
                		propertyStrings += testString + "," + " ";
                	}
                	
		          }
		       }
		       if(propertyStrings.length() > 1) {
			       if(propertyStrings.contains(",")) {
			    	   propertyStrings = propertyStrings.substring(0, propertyStrings.length() - 2);
			       }
			       
			       propertyStrings += "}";
		       }
		       else {
		    	   propertyStrings = "";
		       }
		       
		       
		       testNode = database.createNode(testLabel);
		       for(Property property : getProperties(propertyStrings)) {
		    	   if(property.generatorName().equals("System End-user") || property.generatorName().equals("Decision Stakeholder"))
		    		   testNode.setProperty(property.key(), fakerService.getValue(property));
		    	   else
	    			testNode.setProperty(property.key(), property.generatorName());
	    		}
	    		nodes.add(testNode);
		    }
		    
    	}catch (Exception ioe) {
    		ioe.printStackTrace();
    	}
        
    	return nodes;
    }
    
    public GraphResult generateDecisionCase(String nodeFile, String relationshipFile){
    	List<Relationship> relationships = new ArrayList<>();
        List<Node> nodes = new ArrayList<>();
        List<Property> propertyList;
           
        Label testLabel[] = null;
    	String testString;
    	
    	try {
			OPCPackage pkgn = OPCPackage.open("Decision_Cases\\" + nodeFile);
			XSSFWorkbook wbn = new XSSFWorkbook(pkgn);
		    XSSFSheet sheetn = wbn.getSheetAt(0);
		    XSSFRow rown;
		    XSSFCell celln;
		
		    int MY_MINIMUM_COLUMN_COUNT = 2;
		    
		    int rowStart = 1;
		    int rowEnd = Math.max(1400, sheetn.getLastRowNum());

		    for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
		       rown = sheetn.getRow(rowNum);
		       if (rown == null) {
		          continue;
		       }
		       
		       String propertyStrings = "{";
		       Node testNode;

		       int lastColumn = Math.max(rown.getLastCellNum(), MY_MINIMUM_COLUMN_COUNT);

		       for (int cn = 0; cn < lastColumn; cn++) {
		          celln = rown.getCell(cn, XSSFRow.RETURN_BLANK_AS_NULL);
		          if (celln == null) {} 
		          else {
                	testString = celln.toString();
                	if(cn == 0) {
                		testLabel = LabelsUtil.fromInput(testString);
                	}
                	else {
                		propertyStrings += testString + "," + " ";
                	}
                	
		          }
		       }
		       if(propertyStrings.length() > 1) {
			       if(propertyStrings.contains(",")) {
			    	   propertyStrings = propertyStrings.substring(0, propertyStrings.length() - 2);
			       }
			       
			       propertyStrings += "}";
		       }
		       else {
		    	   propertyStrings = "";
		       }
		       
		       
		       testNode = database.createNode(testLabel);
		       for(Property property : getProperties(propertyStrings)) {
	    			testNode.setProperty(property.key(), property.generatorName());
	    		}
	    		nodes.add(testNode);
		    }
		    
    	}catch (Exception ioe) {
    		ioe.printStackTrace();
    	}
        
        try {
			OPCPackage pkg = OPCPackage.open("Decision_Cases\\" + relationshipFile);
			XSSFWorkbook wb = new XSSFWorkbook(pkg);
		    XSSFSheet sheet = wb.getSheetAt(0);
		    XSSFRow row;
		    XSSFCell cell;
		
		    int MY_MINIMUM_COLUMN_COUNT = 2;
		    
		    int rowStart = 1;
		    int rowEnd = Math.max(1400, sheet.getLastRowNum());

		    for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
		       row = sheet.getRow(rowNum);
		       if (row == null) {
		          continue;
		       }
		       
		       String relationshipType = "", properties = "{", cellString, symbol = "";
		       Node fromNode = null, toNode = null;

		       int lastColumn = Math.max(row.getLastCellNum(), MY_MINIMUM_COLUMN_COUNT);

		       for (int cn = 0; cn < lastColumn; cn++) {
		          cell = row.getCell(cn, XSSFRow.RETURN_BLANK_AS_NULL);
		          if (cell == null) {
		          } else {
                	cellString = cell.toString();
                	
                	if(cn == 0){
                		Node tmp;
                		for(int i = 0; i<nodes.size(); i++) {
                			tmp = nodes.get(i);
                			Object s = tmp.getProperty("name");
                			if(s.equals(cellString)) {
                				fromNode = tmp;
                			}
                		}
                	}
                	else if(cn == 1) {
                		symbol = cellString;
                	}
                	else if(cn == 2) {
                		Node tmp;
                		for(int i = 0; i<nodes.size(); i++) {
                			tmp = nodes.get(i);
                			Object s = tmp.getProperty("name", null);
                			Object t = tmp.getProperty("symbol", null);
                			
                			if(t != null) {
	            				if(t.equals(symbol)){
	                				toNode = tmp;
	                			}	
                			}
                			else if(s != null) {
                				if(s.equals(cellString)) {
                    				toNode = tmp;
                    			}
                			}
                		}
                	}
                	else if(cn == 3) {
                		relationshipType = cellString;
                	}	
                	else if(cn >= 4) {
                		properties += cellString + "," + " ";
                	}	
		          }
		       }
		       if(properties.length() > 1) {
			       if(properties.contains(",")) {
			    	   properties = properties.substring(0, properties.length() - 2);
			       }
			       
			       properties += "}";
		       }
		       else {
		    	   properties = "";
		       }
		       
		       propertyList = getProperties(properties);
		        
		       Relationship r = fromNode.createRelationshipTo(toNode, RelationshipType.withName(relationshipType));
		       addRelationshipProperties(r, propertyList);
		       relationships.add(r);
		    }
    	}catch (Exception ioe) {
    		ioe.printStackTrace();
    	}
        return new GraphResult(nodes, relationships);
    }
    public GraphResult generateArtificialDecisionCases(long number) {
    	List<Relationship> relationships = new ArrayList<>();
    	List<Node> decisionCaseNodes = new ArrayList<>();
    	List<Node> relationshipNodes = new ArrayList<>();
    	
    	
    	if(number > 20)
    		number = 20;
    	
    	
    	for(int i = 0; i < number; i++) {
    		Node tmp;
    		
    		String s = String.valueOf(fakerService.randomLong(3, true));
        	String propertyString = "{name: Case " + s + "}";
        	
    		tmp = database.createNode(Label.label("DECISION_CASE"));
    		for(Property property : getProperties(propertyString)) {
    			tmp.setProperty(property.key(), property.generatorName());
    		}
    		decisionCaseNodes.add(tmp);
    	}
    	
    	
    	relationshipNodes = generateNodesFromFile("Properties_Input.xlsx");
    	
    	for(Node n : decisionCaseNodes) {
    		Iterable<Label> relLabel;
    		String first = relationshipNodes.get(0).getLabels().toString();
    		int relcount = 0;
    		for(int j = 0; j<relationshipNodes.size() ; j++) {
    			Node m = relationshipNodes.get(j);
    			String current = m.getLabels().toString();
    			if(!first.equals(current)) {
    				first = current;
    				if(relcount == 0) {
    					//make relationship
    					Node b = relationshipNodes.get(--j);
    					Relationship r = n.createRelationshipTo(b, RelationshipType.withName(String.valueOf(m.getProperty("relationship"))));
        		    	relationships.add(r);
    				}
    				relcount = 0;
    				
    			}
    			else {
    				int rnd = random.nextInt(100);
	    			if(rnd < 50) {
	    				//make relationship
	    				Relationship r = n.createRelationshipTo(m, RelationshipType.withName(String.valueOf(m.getProperty("relationship"))));
	    		    	relationships.add(r);
	    				relcount++;
	    			}
    			}
    		}
    	}
    	for(Node nds : relationshipNodes) {
    		nds.removeProperty("relationship");
    	}
    	decisionCaseNodes.addAll(relationshipNodes);
    	return new GraphResult(decisionCaseNodes, relationships);
    	
    }


    private List<Property> getProperties(String definition) {
        if (definition.equals("''") || definition.equals("'{}'") || definition.equals("") || definition.equals("{}")) {
            return new ArrayList<>();
        }
        return parser.parse(definition);
    }

    private void addRelationshipProperties(Relationship relationship, List<Property> properties) {
        for (Property property : properties) {
            relationship.setProperty(property.key(), property.generatorName());
        }
    }

}
