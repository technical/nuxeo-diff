/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.diff.model.DiffBlockDefinition;
import org.nuxeo.ecm.diff.model.DiffFieldDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffBlockDefinitionImpl;
import org.nuxeo.ecm.diff.model.impl.DiffFieldDefinitionImpl;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * Tests the contribution to the {@link DiffDisplayService}.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.diff:OSGI-INF/diff-display-service.xml",
        "org.nuxeo.diff:OSGI-INF/diff-display-contrib.xml" })
public class TestDiffDisplayContrib extends TestCase {

    @Inject
    protected DiffDisplayService diffDisplayService;

    /**
     * Tests the diff display contribution.
     */
    @Test
    public void testDiffDisplayContrib() {

        // Check diffDisplay contribs
        Map<String, List<String>> diffDisplays = diffDisplayService.getDiffDisplays();
        assertNotNull(diffDisplays);
        assertEquals(2, diffDisplays.size());
        assertTrue(diffDisplays.containsKey("Document"));
        assertTrue(diffDisplays.containsKey("File"));

        // Check a non existing diffDisplay contrib
        List<String> diffDisplay = diffDisplayService.getDiffDisplay("Test");
        assertNull(diffDisplay);

        // Check default (Document) diffDisplay contrib
        diffDisplay = diffDisplayService.getDefaultDiffDisplay();
        assertNotNull(diffDisplay);

        List<String> expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("header");
        expectedDiffDisplay.add("dublincore");
        assertEquals(expectedDiffDisplay, diffDisplay);

        // Check that order is taken into account
        String diffBlockRef = expectedDiffDisplay.get(0);
        expectedDiffDisplay.remove(0);
        expectedDiffDisplay.add(diffBlockRef);
        assertFalse(expectedDiffDisplay.equals(diffDisplay));

        // Check File diffDisplay contrib
        diffDisplay = diffDisplayService.getDiffDisplay("File");
        assertNotNull(diffDisplay);

        expectedDiffDisplay = new ArrayList<String>();
        expectedDiffDisplay.add("header");
        expectedDiffDisplay.add("dublincore");
        expectedDiffDisplay.add("files");
        assertEquals(expectedDiffDisplay, diffDisplay);
    }

    /**
     * Tests the diff block contribution.
     */
    @Test
    public void testDiffBlockContrib() {

        // Check diffBlock contribs
        Map<String, DiffBlockDefinition> contribs = diffDisplayService.getDiffBlockDefinitions();
        assertNotNull(contribs);
        assertEquals(3, contribs.size());
        assertTrue(contribs.containsKey("header"));
        assertTrue(contribs.containsKey("dublincore"));
        assertTrue(contribs.containsKey("files"));

        // Check a non existing diffBlock contrib
        DiffBlockDefinition diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("test");
        assertNull(diffBlockDefinition);

        // Check header diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("header");
        assertNotNull(diffBlockDefinition);

        List<DiffFieldDefinition> fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("dublincore", "title"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "description"));
        DiffBlockDefinition expectedDiffBlockDefinition = new DiffBlockDefinitionImpl(
                "header", null, fields);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check that order is taken into account
        DiffFieldDefinition diffFieldDefinition = expectedDiffBlockDefinition.getFields().get(
                0);
        expectedDiffBlockDefinition.getFields().remove(0);
        expectedDiffBlockDefinition.getFields().add(diffFieldDefinition);
        assertFalse(expectedDiffBlockDefinition.equals(diffBlockDefinition));

        // Check dublincore diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("dublincore");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        fields.add(new DiffFieldDefinitionImpl("dublincore", "created"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "modified"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "creator"));
        fields.add(new DiffFieldDefinitionImpl("dublincore", "lastContributor"));
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("dublincore",
                null, fields);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);

        // Check files diffDisplay contrib
        diffBlockDefinition = diffDisplayService.getDiffBlockDefinition("files");
        assertNotNull(diffBlockDefinition);

        fields = new ArrayList<DiffFieldDefinition>();
        List<String> items = new ArrayList<String>();
        items.add("filename");
        items.add("file");
        fields.add(new DiffFieldDefinitionImpl("files", "files", items));
        expectedDiffBlockDefinition = new DiffBlockDefinitionImpl("files",
                null, fields);
        assertEquals(expectedDiffBlockDefinition, diffBlockDefinition);
    }
}
