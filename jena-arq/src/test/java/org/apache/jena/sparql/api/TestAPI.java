/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jena.sparql.api;

import java.util.Iterator;

import org.apache.jena.atlas.junit.BaseTest;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;

public class TestAPI extends BaseTest
{
    private static final String ns = "http://example/ns#" ;
    
    static Model m = GraphFactory.makeJenaDefaultModel() ;
    static Resource r1 = m.createResource() ;
    static Property p1 = m.createProperty(ns+"p1") ;
    static Property p2 = m.createProperty(ns+"p2") ;
    static Property p3 = m.createProperty(ns+"p3") ;
    static Model dft = GraphFactory.makeJenaDefaultModel() ;
    static Resource s = dft.createResource(ns+"s") ;
    static Property p = dft.createProperty(ns+"p") ;
    static Resource o = dft.createResource(ns+"o") ;
    static Resource g1 = dft.createResource(ns+"g1") ;
    static Dataset d = null;
    static  {
        m.add(r1, p1, "x1") ;
        m.add(r1, p2, "X2") ; // NB Capital
        m.add(r1, p3, "y1") ;
        dft.add(s, p, o) ;
        d = DatasetFactory.create(dft);
        d.addNamedModel(g1.getURI(), m);
    }
    
    @Test public void testInitialBindingsConstruct1()
    {
        try(QueryExecution qExec = makeQExec("CONSTRUCT {?s ?p ?z} {?s ?p 'x1'}")) {
            QuerySolutionMap init = new QuerySolutionMap() ;
            init.add("z", m.createLiteral("zzz"));
            
            qExec.setInitialBinding(init) ;
            Model r = qExec.execConstruct() ;
        
            assertTrue("Empty model", r.size() > 0 ) ;
        
            Property p1 = m.createProperty(ns+"p1") ;
        
            assertTrue("Empty model", r.contains(null,p1, init.get("z"))) ; 
        }
    }
    
    @Test public void testInitialBindingsConstruct2()
    {
        try(QueryExecution qExec = makeQExec("CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }")) {
            QuerySolutionMap init = new QuerySolutionMap() ;
            init.add("o", m.createLiteral("x1"));
            
            qExec.setInitialBinding(init) ;
            Model r = qExec.execConstruct() ;
        
            assertTrue("Empty model", r.size() > 0 ) ;
        
            Property p1 = m.createProperty(ns+"p1") ;
        
            assertTrue("Empty model", r.contains(null, p1, init.get("x1"))) ; 
        }
    }

    @Test public void test_API1()
    {
        try(QueryExecution qExec = makeQExec("SELECT * {?s ?p ?o}")) {
            ResultSet rs = qExec.execSelect() ;
            assertTrue("No results", rs.hasNext()) ;
            QuerySolution qs = rs.nextSolution() ;
            Resource qr = qs.getResource("s") ;
            assertSame("Not the same model as queried", qr.getModel(), m) ;
        }
    }
    
//    @Test public void test_OptRegex1()
//    {
//        execRegexTest(1, "SELECT * {?s ?p ?o . FILTER regex(?o, '^x')}") ;
//    }
//
//    @Test public void test_OptRegex2()
//    {
//        execRegexTest(2, "SELECT * {?s ?p ?o . FILTER regex(?o, '^x', 'i')}") ;
//    }

    @Test public void testInitialBindings0()
    {
        QuerySolutionMap smap1 = new QuerySolutionMap() ;
        QuerySolutionMap smap2 = new QuerySolutionMap() ;
        smap1.add("o", m.createLiteral("y1"));
        smap2.addAll(smap1) ;
        assertTrue(smap2.contains("o")) ;
        smap2.clear() ;
        assertFalse(smap2.contains("o")) ;
        assertTrue(smap1.contains("o")) ;
        
        QuerySolutionMap smap3 = new QuerySolutionMap() ;
        smap2.addAll((QuerySolution)smap1) ;
        assertTrue(smap2.contains("o")) ;
    }
    
    @Test public void testInitialBindings1()
    {
        QueryExecution qExec = makeQExec("SELECT * {?s ?p ?o}") ;
        QuerySolutionMap init = new QuerySolutionMap() ;
        init.add("o", m.createLiteral("y1"));
        qExec.setInitialBinding(init) ;
        int count = queryAndCount(qExec) ;
        assertEquals("Initial binding didn't restrict query properly", 1, count) ;
    }
    
    @Test public void testInitialBindings2()
    {
        QueryExecution qExec = makeQExec("SELECT * {?s ?p ?o}") ;
        QuerySolutionMap init = new QuerySolutionMap() ;
        init.add("z", m.createLiteral("zzz"));
        qExec.setInitialBinding(init) ;
        int count = queryAndCount(qExec) ;
        assertEquals("Initial binding restricted query improperly", 3, count) ;
    }

    @Test public void testInitialBindings3()
    {
        try(QueryExecution qExec = makeQExec("SELECT * {?s ?p 'x1'}")) {
            QuerySolutionMap init = new QuerySolutionMap() ;
            init.add("z", m.createLiteral("zzz"));
            qExec.setInitialBinding(init) ;
            ResultSet rs = qExec.execSelect() ;
            QuerySolution qs = rs.nextSolution() ;
            assertTrue("Initial setting not set correctly now", qs.getLiteral("z").getLexicalForm().equals("zzz")) ;
        }
    }
    
    @Test public void testInitialBindings4()
    {
        // Test derived from report by Holger Knublauch
        String queryString =
            "PREFIX : <"+ns+">\n" +
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
            "SELECT * \n" +
            "WHERE { \n" +
            "    ?x :p1 ?z ." +
            "    NOT EXISTS { \n" +
            "        ?x rdfs:label ?z . \n" +
            "    }\n" +
            "}";
        
        Query query = QueryFactory.create(queryString, Syntax.syntaxARQ);
        try(QueryExecution qexec = QueryExecutionFactory.create(query, m)) {
            QuerySolutionMap map = new QuerySolutionMap();
            map.add("this", OWL.Thing);
            qexec.setInitialBinding(map);
            
            ResultSet rs = qexec.execSelect();
            while(rs.hasNext()) {
                QuerySolution qs = rs.nextSolution();
                //System.out.println("Result: " + qs);
            }
        }
    }
    
    /**
     * Initial binding substitution happens before optimization so initial bindings can make a semantically always false query into one that can return true
     */
    @Test public void testInitialBindings5() {
        // From JENA-500
        Query query = QueryFactory.create(
                "ASK\n" +
                "WHERE {\n" +
                "    FILTER (?a = <http://constant>) .\n" +
                "}");
        //System.out.println(Algebra.optimize(Algebra.compile(query)).toString());
        
        Model model = ModelFactory.createDefaultModel();
        model.add(OWL.Thing, RDF.type, OWL.Class);
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add("a", ResourceFactory.createResource("http://constant"));
        QueryExecution qexec = QueryExecutionFactory.create(query, model, initialBinding);
        boolean result = qexec.execAsk();
        assertTrue(result);
    }
    
    /**
     * Initial binding substitution happens before optimization so initial bindings can make a semantically always false query into one that can return true
     */
    @Test public void testInitialBindings6() {
        // From JENA-500
        Query query = QueryFactory.create(
                "ASK\n" +
                "WHERE {\n" +
                "    FILTER (?a = ?b) .\n" +
                "}");
        //System.out.println(Algebra.optimize(Algebra.compile(query)).toString());
        
        Model model = ModelFactory.createDefaultModel();
        model.add(OWL.Thing, RDF.type, OWL.Class);
        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add("a", ResourceFactory.createTypedLiteral(Boolean.TRUE));
        initialBinding.add("b", ResourceFactory.createTypedLiteral(Boolean.TRUE));
        QueryExecution qexec = QueryExecutionFactory.create(query, model, initialBinding);
        boolean result = qexec.execAsk();
        assertTrue(result);
    }
    
    @Test public void testReuseQueryObject1()
    {
        String queryString = "SELECT * {?s ?p ?o}";
        Query q = QueryFactory.create(queryString) ;
        
        QueryExecution qExec = QueryExecutionFactory.create(q, m) ;
        int count = queryAndCount(qExec) ;
        assertEquals(3, count) ;
        
        qExec = QueryExecutionFactory.create(q, m) ;
        count = queryAndCount(qExec) ;
        assertEquals(3, count) ;
    }
    
    
    @Test public void testReuseQueryObject2()
    {
        String queryString = "SELECT (count(?s) AS ?c) {?s ?p ?o} GROUP BY ?s";
        Query q = QueryFactory.create(queryString) ;
        
        try(QueryExecution qExec = QueryExecutionFactory.create(q, m)) {
            
            ResultSet rs = qExec.execSelect() ;
            QuerySolution qs = rs.nextSolution() ;
            assertEquals(3, qs.getLiteral("c").getInt()) ;
        }
            
        try(QueryExecution qExec = QueryExecutionFactory.create(q, m)) {
            ResultSet rs = qExec.execSelect() ;
            QuerySolution qs = rs.nextSolution() ;
            assertEquals(3, qs.getLiteral("c").getInt()) ;
        }
    }
    
    @Test public void testConstructRejectsBadTriples1()
    {
        String queryString = "CONSTRUCT { ?s ?p ?o } WHERE { ?o ?p ?s }";
        Query q = QueryFactory.create(queryString);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, m);
        
        Model resultModel = qExec.execConstruct();
        assertEquals(0, resultModel.size());
    }
    
    @Test public void testConstructRejectsBadTriples2()
    {
        String queryString = "CONSTRUCT { ?s ?p ?o } WHERE { ?o ?p ?s }";
        Query q = QueryFactory.create(queryString);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, m);
        
        Iterator<Triple> ts = qExec.execConstructTriples();
        long count = 0;
        while (ts.hasNext()) {
            count++;
            ts.next();
        }
        assertEquals(0, count);
    }
    
    
//    // Execute a test both with and without regex optimization enabled
//    // Check the number of results
//    private void XexecRegexTest(int expected, String queryString)
//    {
//        Object b = ARQ.getContext().get(ARQ.enableRegexConstraintsOpt) ;
//        try {
//            ARQ.getContext().set(ARQ.enableRegexConstraintsOpt, "false") ;
//            int count1 = queryAndCount(queryString) ;
//            ARQ.getContext().set(ARQ.enableRegexConstraintsOpt, "true") ;
//            int count2 = queryAndCount(queryString) ;
//            assertEquals("Different number of results", count1, count2) ;
//            if ( expected >= 0 )
//                assertEquals("Unexpected number of results", expected, count1) ;
//        } finally {
//            ARQ.getContext().set(ARQ.enableRegexConstraintsOpt, b) ;
//        }
//    }
    
    // ARQ Construct Quad Tests:
    // Two types of query strings: a) construct triple string; b) construct quad string;
    // Two kinds of query methods: 1) execTriples(); 2) execQuads();
    
    // Test a)+1)
    @Test public void testARQConstructQuad_a_1() {
        String queryString = "CONSTRUCT { ?s ?p ?o } WHERE { GRAPH ?g { ?s ?p ?o } }";
        Query q = QueryFactory.create(queryString, Syntax.syntaxARQ);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, d);
        
        Iterator<Triple> ts = qExec.execConstructTriples();
        Model result = ModelFactory.createDefaultModel();
        while (ts.hasNext()) {
            Triple t = ts.next();
            Statement stmt = ModelUtils.tripleToStatement(result, t);
            if ( stmt != null )
                result.add(stmt);
        }
        assertEquals(3, result.size());
        assertTrue(m.isIsomorphicWith(result));
    }
    
    // Test b)+2)
    @Test public void testARQConstructQuad_b_2() {
        String queryString = "CONSTRUCT { GRAPH ?g1 {?s ?p ?o} } WHERE { ?s ?p ?o. GRAPH ?g1 {?s1 ?p1 'x1'} }";
        Query q = QueryFactory.create(queryString, Syntax.syntaxARQ);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, d);
        
        Iterator<Quad> ts = qExec.execConstructQuads();
        DatasetGraph result = DatasetGraphFactory.createMem();
        long count = 0;
        while (ts.hasNext()) {
            count++;
            Quad qd = ts.next();
            result.add(qd);
        }
        
        DatasetGraph expected = DatasetGraphFactory.createMem();
        expected.add(g1.asNode(), s.asNode(), p.asNode(), o.asNode());
        
        assertEquals(1, count); // 3 duplicated quads
        assertTrue(IsoMatcher.isomorphic( expected, result) );
        
    }
    
    // Test a)+2): Quads constructed in the default graph
    @Test public void testARQConstructQuad_a_2() {
        String queryString = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
        Query q = QueryFactory.create(queryString, Syntax.syntaxARQ);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, d);
        
        Iterator<Quad> ts = qExec.execConstructQuads();
        DatasetGraph result = DatasetGraphFactory.createMem();
        long count = 0;
        while (ts.hasNext()) {
            count++;
            result.add( ts.next() );
        }
        DatasetGraph expected = DatasetGraphFactory.createMem();
        expected.add(Quad.defaultGraphNodeGenerated, s.asNode(), p.asNode(), o.asNode());
        assertEquals(1, count);
        assertTrue(IsoMatcher.isomorphic( expected, result) );
        
    }
    
    // Test b)+1): Projection on default graph, ignoring constructing named graphs
    @Test public void testARQConstructQuad_b_1() {
        String queryString = "CONSTRUCT { ?s ?p ?o GRAPH ?g1 { ?s1 ?p1 ?o1 } } WHERE { ?s ?p ?o. GRAPH ?g1 { ?s1 ?p1 ?o1 } }";
        Query q = QueryFactory.create(queryString, Syntax.syntaxARQ);
        
        QueryExecution qExec = QueryExecutionFactory.create(q, d);
        
        Iterator<Triple> ts = qExec.execConstructTriples();
        Model result = ModelFactory.createDefaultModel();
        while (ts.hasNext()) {
            Triple t = ts.next();
            Statement stmt = ModelUtils.tripleToStatement(result, t);
            if ( stmt != null )
                result.add(stmt);
        }
        assertEquals(1, result.size());
        assertTrue(dft.isIsomorphicWith(result));
    }
    
    private QueryExecution makeQExec(String queryString)
    {
        Query q = QueryFactory.create(queryString) ;
        QueryExecution qExec = QueryExecutionFactory.create(q, m) ;
        return qExec ;
    }

    private int queryAndCount(String queryString)
    {
        QueryExecution qExec = makeQExec(queryString) ;
        return queryAndCount(qExec) ;
    }

    
    private int queryAndCount(QueryExecution qExec)
    {
        try {
            ResultSet rs = qExec.execSelect() ;
            return ResultSetFormatter.consume(rs) ;
        } finally { qExec.close() ; }
    }
}
