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

package generate;

import generate.result.GraphResult;
import generate.result.ValueListResult;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.PerformsWrites;
import org.neo4j.procedure.Procedure;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class GenerateDecisionCaseProcedure extends GraphgenProcedure {

	@Procedure
    @PerformsWrites
    public Stream<GraphResult> generateDecisionCase(@Name("file") String nodeFileName, @Name("file2") String relationshipsFileName) {
        return Stream.of(getGraphgenService().graphGenerator().generateDecisionCase(nodeFileName, relationshipsFileName));
    }
}
