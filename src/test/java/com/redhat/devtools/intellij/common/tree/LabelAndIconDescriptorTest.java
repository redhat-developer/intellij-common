/*******************************************************************************
 * Copyright (c) 2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.redhat.devtools.intellij.common.tree;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.PresentableNodeDescriptor;
import com.intellij.ui.SimpleTextAttributes;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class LabelAndIconDescriptorTest {

    @Test
    public void checkLinkWithoutHref() {
        assertTrue(LabelAndIconDescriptor.HREF_PATTERN.matcher("Click <a>here</a>.").find());
    }

    @Test
    public void checkLinkWithHref() {
        assertTrue(LabelAndIconDescriptor.HREF_PATTERN.matcher("Click <a href=\"xxxx\">here</a>.").find());
    }

    @Test
    public void checkTextWithoutHyperLink() {
        LabelAndIconDescriptor descriptor = new LabelAndIconDescriptor(null, null, "mytext", null, null);
        PresentationData data = new PresentationData();
        descriptor.update(data);
        assertEquals("mytext", data.getPresentableText());

    }

    private void testTextWithOneHyperlink(String text) {
        LabelAndIconDescriptor descriptor = new LabelAndIconDescriptor(null, null, text, null, null);
        PresentationData data = new PresentationData();
        descriptor.update(data);
        assertNull(data.getPresentableText());
        List<PresentableNodeDescriptor.ColoredFragment> fragments = data.getColoredText();
        assertNotNull(fragments);
        assertEquals(3, fragments.size());
        assertEquals("Click ", fragments.get(0).getText());
        assertEquals(SimpleTextAttributes.REGULAR_ATTRIBUTES, fragments.get(0).getAttributes());
        assertEquals("here", fragments.get(1).getText());
        assertEquals(LabelAndIconDescriptor.LINK_ATTRIBUTES, fragments.get(1).getAttributes());
        assertEquals(".", fragments.get(2).getText());
        assertEquals(SimpleTextAttributes.REGULAR_ATTRIBUTES, fragments.get(2).getAttributes());
    }

    @Test
    public void checkTextWithOneHyperLinkAndNoHref() {
        testTextWithOneHyperlink("Click <a>here</a>.");
    }

    @Test
    public void checkTextWithOneHyperLinkAndHref() {
        testTextWithOneHyperlink("Click <a href=\"xxx\">here</a>.");
    }

    private void testTextWithTwoHyperlinks(String text) {
        LabelAndIconDescriptor descriptor = new LabelAndIconDescriptor(null, null, text, null, null);
        PresentationData data = new PresentationData();
        descriptor.update(data);
        assertNull(data.getPresentableText());
        List<PresentableNodeDescriptor.ColoredFragment> fragments = data.getColoredText();
        assertNotNull(fragments);
        assertEquals(5, fragments.size());
        assertEquals("Click ", fragments.get(0).getText());
        assertEquals(SimpleTextAttributes.REGULAR_ATTRIBUTES, fragments.get(0).getAttributes());
        assertEquals("here", fragments.get(1).getText());
        assertEquals(LabelAndIconDescriptor.LINK_ATTRIBUTES, fragments.get(1).getAttributes());
        assertEquals(" or ", fragments.get(2).getText());
        assertEquals(SimpleTextAttributes.REGULAR_ATTRIBUTES, fragments.get(2).getAttributes());
        assertEquals("there", fragments.get(3).getText());
        assertEquals(LabelAndIconDescriptor.LINK_ATTRIBUTES, fragments.get(3).getAttributes());
        assertEquals(".", fragments.get(4).getText());
        assertEquals(SimpleTextAttributes.REGULAR_ATTRIBUTES, fragments.get(4).getAttributes());
    }

    @Test
    public void checkTextWithTwoHyperLinkAndNoHref() {
        testTextWithTwoHyperlinks("Click <a>here</a> or <a>there</a>.");
    }

    @Test
    public void checkTextWithTwoHyperLinkAndHref() {
        testTextWithTwoHyperlinks("Click <a href=\"xxx\">here</a> or <a>there</a>.");
    }
}
