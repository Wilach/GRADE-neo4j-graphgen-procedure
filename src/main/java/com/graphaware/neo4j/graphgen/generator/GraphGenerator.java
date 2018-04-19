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


    public List<Relationship> generateRelationships(List<Node> from, List<Node> to, String relationshipType, String properties, String fromCount, String toCount) {
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

    public GraphResult generateLinkedList(List<Node> nodes, String relationshipType) {
        List<Relationship> relationships = new ArrayList<>();
        int i = 0;
        while (i < nodes.size() - 1) {
            relationships.add(nodes.get(i).createRelationshipTo(nodes.get(++i), RelationshipType.withName(relationshipType)));
        }

        return new GraphResult(nodes, relationships);
    }
    
    public List<Node> asdGenerateNodeTest(Label[] labels, String propertiesString, long number){
    	Label testLabel[] = null;
    	String testString;
    	List<Node> nodes = new ArrayList<>();
    	try {
    	OPCPackage pkg = OPCPackage.open("C:\\Users\\ownzo\\Documents\\GitHub\\GRADE-neo4j-graphgen-procedure\\test.xlsx");
    	XSSFWorkbook wb = new XSSFWorkbook(pkg);
        XSSFSheet sheet = wb.getSheetAt(0);
        XSSFRow row;
        XSSFCell cell;

        int rows; // No of rows
        rows = sheet.getPhysicalNumberOfRows();

        int cols = 0; // No of columns
        int tmp = 0;

        // This trick ensures that we get the data properly even if it doesn't start from first few rows
        for(int i = 0; i < 10 || i < rows; i++) {
            row = sheet.getRow(i);
            if(row != null) {
                tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                if(tmp > cols) cols = tmp;
            }
        }
        
        for(int r = 0; r < rows; r++) {
            row = sheet.getRow(r);
            String propertyStrings = "";
            Node testNode;
            if(row != null) {
                for(int c = 0; c < cols; c++) {
                    cell = row.getCell(c);
                    if(cell != null) {
                        // Your code here
                    	testString = cell.toString();
                    	if(c == 0) {
                    		testLabel = LabelsUtil.fromInput(testString);
                    	}
                    	else {
                    		propertyStrings += testString + " ";
                    	}
                    }
                }
                testNode = database.createNode(testLabel);
        		for(Property property : getProperties(propertyStrings)) {
        			testNode.setProperty(property.key(), property);
        		}
        		nodes.add(testNode);
            }
        }
        pkg.close();
    	}catch (Exception ioe) {
    		ioe.printStackTrace();
    	}
    	/* 
         for (int i = 0; i < number; ++i) {
             Node node = database.createNode(labels);
             for (Property property : getProperties(propertiesString)) {
                 node.setProperty(property.key(), testString);
             }
             nodes.add(node);
         }*/

         return nodes;
    }

    private List<Property> getProperties(String definition) {
        if (definition.equals("''") || definition.equals("'{}'") || definition.equals("") || definition.equals("{}")) {
            return new ArrayList<>();
        }
        return parser.parse(definition);
    }

    private void addRelationshipProperties(Relationship relationship, List<Property> properties) {
        for (Property property : properties) {
            relationship.setProperty(property.key(), fakerService.getValue(property));
        }
    }

}
