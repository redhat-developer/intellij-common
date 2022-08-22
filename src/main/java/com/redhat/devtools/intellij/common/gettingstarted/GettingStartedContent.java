/*******************************************************************************
 * Copyright (c) 2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc.
 ******************************************************************************/
package com.redhat.devtools.intellij.common.gettingstarted;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.impl.ContentImpl;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.tree.TreeUtil;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.util.Map;

public class GettingStartedContent extends ContentImpl {

    protected ToolWindow toolWindow;
    protected GettingStartedCourse course;
    protected JPanel mainPanel;

    public GettingStartedContent(ToolWindow toolWindow, String displayName, GettingStartedCourse course) {
        super(null, displayName, true);
        this.toolWindow = toolWindow;
        this.course = course;
        mainPanel = new JPanel(new BorderLayout());
        fillWithLessonsList(-1);
        setComponent(new JBScrollPane(mainPanel));
    }

    private void fillWithLessonsList(int rowToExpandWhenExitingLesson) {
        emptyMainPanel();

        JPanel titlePanel = createCourseTitlePanel();
        mainPanel.add(titlePanel, BorderLayout.NORTH);

        JTree tree = createLessonsTree(rowToExpandWhenExitingLesson);
        mainPanel.add(tree, BorderLayout.CENTER);

        URL feedbackLink = course.getUserRedirectForFeedback();
        if (feedbackLink != null) {
            JPanel feedbackPanel = createAskFeedbackPanel(feedbackLink);
            mainPanel.add(feedbackPanel, BorderLayout.SOUTH);
        }

        refresh();
    }

    /**
     This creates the panel seen on the top of the main page of a course.
     It contains the course name and its short description
     */
    private JPanel createCourseTitlePanel() {
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBorder(JBUI.Borders.emptyBottom(20));

        JLabel courseTitleLabel = createLabel(course.getTitle(), true, Font.BOLD, 16);
        titlePanel.add(courseTitleLabel);

        String courseShortDescription = course.getShortDescription();
        if (!courseShortDescription.isEmpty()) {
            JLabel courseDescriptionLabel = createLabel(courseShortDescription, false, -1, -1);
            titlePanel.add(courseDescriptionLabel);
        }
        return titlePanel;
    }

    /**
     This creates the tree containing all lessons belonging to a course.
     It is part of the main page of a course.
     It consists of two types of nodes:
     - group of lessons
     - actual lesson
     */
    private JTree createLessonsTree(int rowToExpandWhenExitingLesson) {
        // if the course only has a group of lessons, the group node is expanded by default
        if (course.getGroupLessons().size() == 1) {
            rowToExpandWhenExitingLesson = 0;
        }

        DefaultTreeModel model = createTreeModel();
        JTree tree = buildTree(model);
        if (rowToExpandWhenExitingLesson != -1) {
            tree.expandRow(rowToExpandWhenExitingLesson);
        }
        tree.setBorder(JBUI.Borders.emptyLeft(10));
        return tree;
    }

    /**
     This creates the panel seen on the bottom of the main page of a course.
     It contains the link for sending feedbacks
     */
    private JPanel createAskFeedbackPanel(URL feedbackLink) {
        JPanel feedbackPanel = new JPanel();
        feedbackPanel.setLayout(new BoxLayout(feedbackPanel, BoxLayout.Y_AXIS));

        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        Border innerBorder = JBUI.Borders.emptyTop(10);
        Border lineSeparatorBelow = new MatteBorder(1, 0, 0, 0, JBUI.CurrentTheme.Tooltip.borderColor());
        Border compoundBorderMargin = BorderFactory.createCompoundBorder(lineSeparatorBelow, innerBorder);
        linkPanel.setBorder(compoundBorderMargin);
        linkPanel.add(createLink("Leave feedback", () -> BrowserUtil.browse(feedbackLink)));

        feedbackPanel.add(linkPanel);

        JPanel feedbackMessagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel feedbackMessageLabel = createLabel("Help us making this plugin better for everyone", false, -1, -1);
        feedbackMessagePanel.add(feedbackMessageLabel);

        feedbackPanel.add(feedbackMessagePanel);
        return feedbackPanel;
    }

    /**
     This creates the panel seen when clicking on a lesson link.
     It contains the actual content of a lesson
     navigation links (go back to course page, go to previous/next lesson)
     title
     description
     action buttons
     animated image
     */
    private void fillWithLessonContent(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson) {
        emptyMainPanel();

        mainPanel.add(createTopNavigationPanel(group, lesson, rowToExpandWhenExitingLesson), BorderLayout.NORTH);
        mainPanel.add(createLessonContentPanel(lesson), BorderLayout.CENTER);

        refresh();
    }

    /**
     * This creates the panel seen on the top of a lesson page.
     * It contains the link for going back to the course page and the links to go to the previous/next lesson, if any
     * @param group belonging group
     * @param lesson actual lesson
     * @param rowToExpandWhenExitingLesson row of the tree of the course in main page
     * @return
     */
    private JPanel createTopNavigationPanel(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson) {
        JLabel backToMenuLink = createLink(
                "<< " + group.getTitle(),
                () -> fillWithLessonsList(rowToExpandWhenExitingLesson));

        JPanel lessonNavigationPanel = createLessonNavigationPanel(group, lesson, rowToExpandWhenExitingLesson);

        JPanel navigationPanel = new JPanel(new BorderLayout());
        Border innerBorder = JBUI.Borders.emptyBottom(5);
        Border lineSeparatorBelow = new MatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.Tooltip.borderColor());
        Border compoundBorderMargin = BorderFactory.createCompoundBorder(lineSeparatorBelow, innerBorder);
        navigationPanel.setBorder(compoundBorderMargin);
        navigationPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 35));

        navigationPanel.add(backToMenuLink, BorderLayout.CENTER);
        navigationPanel.add(lessonNavigationPanel, BorderLayout.EAST);
        return navigationPanel;
    }

    /**
     * This creates the panel seen on the top-right of a lesson page.
     * This contains the links to go to a previous/next lesson, if any
     * @param group belonging group
     * @param lesson actual lesson
     * @param rowToExpandWhenExitingLesson row of the tree of the course in main page
     * @return
     */
    private JPanel createLessonNavigationPanel(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson) {
        GettingStartedLesson previousLesson = null, nextLesson = null;
        int numberOfLessons = group.getLessons().size();
        for (int i = 0; i<numberOfLessons; i++) {
            GettingStartedLesson currentLesson = group.getLessons().get(i);
            if (currentLesson.equals(lesson)) {
                if (i != 0) {
                    previousLesson = group.getLessons().get(i - 1);
                }
                if (i != numberOfLessons - 1) {
                    nextLesson = group.getLessons().get(i + 1);
                }
            }
        }

        JPanel lessonNavigationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        if (previousLesson != null) {
            lessonNavigationPanel.add(createPreviousLessonLink(group, previousLesson, rowToExpandWhenExitingLesson));
        }
        if (nextLesson != null) {
            lessonNavigationPanel.add(createNextLessonLink(group, nextLesson, rowToExpandWhenExitingLesson));
        }

        return lessonNavigationPanel;
    }

    private JLabel createPreviousLessonLink(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson) {
        return createOtherLessonLink(group, lesson, rowToExpandWhenExitingLesson, false);
    }

    private JLabel createNextLessonLink(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson) {
        return createOtherLessonLink(group, lesson, rowToExpandWhenExitingLesson, true);
    }

    private JLabel createOtherLessonLink(GettingStartedGroupLessons group, GettingStartedLesson lesson, int rowToExpandWhenExitingLesson, boolean isNext) {
        return createLink(isNext
                        ? "Next: " + lesson.getTitle() + " >"
                        : "< Back: " + lesson.getTitle(),
                () -> fillWithLessonContent(group, lesson, rowToExpandWhenExitingLesson));
    }

    private JLabel createLink(String text, Runnable runnable) {
        JLabel link = new JLabel(text);
        link.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    adaptLabelAsLink((JLabel)e.getSource(), TextAttribute.UNDERLINE_ON);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                adaptLabelAsLink((JLabel)e.getSource(), -1);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                runnable.run();
            }
        });
        return link;
    }

    private void adaptLabelAsLink(JLabel label, int underlineAttribute) {
        Font font = label.getFont();
        Map attributes = font.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, underlineAttribute);
        label.setFont(font.deriveFont(attributes));
    }

    /**
     * This creates the panel containing the actual content of a lesson
     *  title
     *  description
     *  action buttons
     *  animated image
     * @param lesson
     * @return
     */
    private JComponent createLessonContentPanel(GettingStartedLesson lesson) {
        JPanel lessonContentPanel = new JPanel(new BorderLayout());
        lessonContentPanel.add(createTitleDescriptionAndButtonsPanel(lesson), BorderLayout.NORTH);
        lessonContentPanel.add(createAnimatedImagePanel(lesson.getAnimatedImage()), BorderLayout.CENTER);
        return lessonContentPanel;
    }

    /**
     * This creates the panel containing the title, desciption and action buttons in a lesson page
     * @param lesson
     * @return
     */
    private JPanel createTitleDescriptionAndButtonsPanel(GettingStartedLesson lesson) {
        JPanel titleDescriptionAndButtonsPanel = new JPanel();
        titleDescriptionAndButtonsPanel.setLayout(new BoxLayout(titleDescriptionAndButtonsPanel, BoxLayout.Y_AXIS));

        JPanel lessonTitlePanel = createLessonTitlePanel(lesson.getTitle(), lesson.getDescription().contains("<html>"));
        titleDescriptionAndButtonsPanel.add(lessonTitlePanel);

        JEditorPane editorPanel = createLessonDescriptionPanel(lesson.getDescription());
        titleDescriptionAndButtonsPanel.add(editorPanel);
        titleDescriptionAndButtonsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        for (Action action : lesson.getActions()) {
            JButton button = new JButton((String) action.getValue(Action.NAME));
            button.setAction(action);
            button.setAlignmentX(Component.CENTER_ALIGNMENT);
            titleDescriptionAndButtonsPanel.add(button);
            titleDescriptionAndButtonsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        return titleDescriptionAndButtonsPanel;
    }

    /**
     * This creates the panel containing the lesson title displayed in a lesson page
     * @param title lesson title
     * @param isDescriptionInHtml true if lesson description has a html tag
     * @return
     */
    private JPanel createLessonTitlePanel(String title, boolean isDescriptionInHtml) {
        JPanel lessonTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel courseTitleLabel = createLabel(title, true, Font.BOLD, 20);
        lessonTitlePanel.setBorder(JBUI.Borders.emptyTop(15));
        if (!isDescriptionInHtml) {
            lessonTitlePanel.setBorder(JBUI.Borders.empty(15, 0, 10, 0)); // this is needed if user pass a simple string
        }
        lessonTitlePanel.add(courseTitleLabel);
        return lessonTitlePanel;
    }

    /**
     * This creates the panel containing the lesson description displayed in a lesson page
     * @param description lesson description
     * @return
     */
    private JEditorPane createLessonDescriptionPanel(String description) {
        JEditorPane editorPanel = new JEditorPane("text/html", description);
        editorPanel.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPanel.setFont(new JLabel().getFont());
        editorPanel.setForeground(new JLabel().getForeground());
        editorPanel.setEditable(false);
        editorPanel.setOpaque(false);
        editorPanel.setBorder(null);
        return editorPanel;
    }

    private JPanel createAnimatedImagePanel(URL animatedImageURL) {
        ImageIcon imgIcon = new ImageIcon(animatedImageURL);
        return new JPanel() {

            @Override
            public Dimension getPreferredSize() {
                int imageWidth = imgIcon.getIconWidth();
                double widthScale = (double)getWidth() / (double)imageWidth;
                int imageHeight = (int) (imgIcon.getIconHeight() * widthScale);
                return new Dimension(imageWidth, imageHeight);
            }

            @Override
            protected void paintComponent(Graphics g) {
                int imageWidth = imgIcon.getIconWidth();
                int imageHeight = imgIcon.getIconHeight();

                if (imageWidth == 0 || imageHeight == 0) {
                    return;
                }
                double widthScale = (double)getWidth() / (double)imageWidth;
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.drawImage(imgIcon.getImage(), AffineTransform.getScaleInstance(widthScale, widthScale), this);
                g2d.dispose();
            }
        };
    }

    private void emptyMainPanel() {
        mainPanel.removeAll();
        mainPanel.setBorder(JBUI.Borders.empty(20));
    }

    private void refresh() {
        mainPanel.repaint();
        mainPanel.revalidate();
    }

    private Tree buildTree(TreeModel treeModel) {
        Tree tree = new Tree(treeModel);
        tree.setCellRenderer(getTreeCellRenderer());

        final int[] lastSelected = {-1};
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                JTree tree=(JTree) e.getSource();
                lastSelected[0] = -1;
                tree.clearSelection();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                JTree tree=(JTree) e.getSource();
                int selRow=tree.getRowForLocation(e.getX(), e.getY());
                if(selRow==-1){
                    tree.clearSelection();
                    lastSelected[0]=-1;
                }
                else if(selRow != lastSelected[0]){
                    tree.setSelectionRow(selRow);
                    lastSelected[0]=selRow;
                }
            }
        });

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                int selRow = tree.getRowForLocation(p.x, p.y);
                TreeCellRenderer r = tree.getCellRenderer();
                if (selRow != -1 && r != null) {
                    TreePath path = tree.getPathForRow(selRow);
                    Object lastPath = path.getLastPathComponent();
                    if (lastPath instanceof DefaultMutableTreeNode) {
                        Object userObject = ((DefaultMutableTreeNode) lastPath).getUserObject();
                        if (userObject instanceof GettingStartedLesson) {
                            Object parentLastPath = ((DefaultMutableTreeNode) lastPath).getParent();
                            if (parentLastPath instanceof DefaultMutableTreeNode) {
                                Object parentUserObject = ((DefaultMutableTreeNode) parentLastPath).getUserObject();
                                if (parentUserObject instanceof GettingStartedGroupLessons) {
                                    int rowToExpandWhenExitingLesson = tree.getRowForPath(path.getParentPath());
                                    fillWithLessonContent((GettingStartedGroupLessons)parentUserObject, (GettingStartedLesson)userObject, rowToExpandWhenExitingLesson);
                                }
                            }

                        }
                    }
                }
            }
        });

        tree.setVisible(true);
        tree.setRootVisible(false);
        return tree;
    }

    private DefaultTreeModel createTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();

        for (GettingStartedGroupLessons groupLessons: course.getGroupLessons()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupLessons);

            for (GettingStartedLesson lesson: groupLessons.getLessons()) {
                DefaultMutableTreeNode lessonNode = new DefaultMutableTreeNode(lesson);
                groupNode.add(lessonNode);
            }
            root.add(groupNode);
        }
        return new DefaultTreeModel(root);
    }

    private TreeCellRenderer getTreeCellRenderer() {
        return (tree1, value, selected, expanded, leaf, row, hasFocus) -> {
            Object node = TreeUtil.getUserObject(value);
            if (node instanceof GettingStartedGroupLessons) {
                tree1.setCursor(Cursor.getDefaultCursor());
                return createLessonsGroupLabel((GettingStartedGroupLessons) node);
            }
            if (node instanceof GettingStartedLesson) {
                tree1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return createLessonLabel((GettingStartedLesson)node, hasFocus);
            }
            return null;
        };
    }

    private JComponent createLessonsGroupLabel(GettingStartedGroupLessons group) {

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBorder(JBUI.Borders.empty(5, 0));

        JLabel courseTitleLabel = createLabel(group.getTitle(), true, Font.BOLD, -1);
        titlePanel.add(courseTitleLabel, BorderLayout.CENTER);

        JLabel lessonDescriptionLabel = createLabel(group.getShortDescription(), false, -1, -1);
        titlePanel.add(lessonDescriptionLabel, BorderLayout.SOUTH);

        return titlePanel;
    }

    private JComponent createLessonLabel(GettingStartedLesson lesson, boolean hasFocus) {

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));


        JLabel lessonTitleLabel = new JLabel(lesson.getTitle());
        lessonTitleLabel.setForeground(JBUI.CurrentTheme.Link.Foreground.ENABLED);

        titlePanel.setBorder(JBUI.Borders.empty(5, 0));
        titlePanel.add(lessonTitleLabel);

        if (hasFocus) {
            titlePanel.setBackground(JBUI.CurrentTheme.StatusBar.hoverBackground());
        }

        return titlePanel;
    }

    private JLabel createLabel(String text, boolean enabled, int style, float size) {
        JLabel label = new JLabel(text);
        label.setEnabled(enabled);
        if (style == -1) {
            style = label.getFont().getStyle();
        }
        if (size == -1) {
            size = label.getFont().getSize();
        }
        label.setFont(label.getFont().deriveFont(style, size));
        return label;
    }
}

