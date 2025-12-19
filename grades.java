/**
 * StudentGradeTrackerAdvanced.java
 *
 * Single-file Swing application that demonstrates:
 * - Add / Search / Update / Delete students
 * - Store grades with int[] (array) and students in ArrayList<Student>
 * - Calculate average, highest, lowest
 * - Sort by Name, Average, Highest, Lowest
 * - Show bar chart of student averages (JFreeChart)
 * - Export a PDF report that includes the textual report and an embedded chart (PDFBox)
 *
 * Dependencies (Maven coordinates shown below):
 *  - org.jfree:jfreechart
 *  - org.apache.pdfbox:pdfbox
 *
 * Compile & run instructions provided after the code block.
 */

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

// JFreeChart classes
import org.jfree.chart.*;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.ChartUtils;

// PDFBox classes
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

public class StudentGradeTrackerAdvanced extends JFrame {

    // Model classes
    static class Student {
        String name;
        int[] grades; // use array for grades

        Student(String name, int[] grades) {
            this.name = name;
            this.grades = grades;
        }

        double getAverage() {
            if (grades == null || grades.length == 0) return 0;
            int sum = 0;
            for (int g : grades) sum += g;
            return (double) sum / grades.length;
        }

        int getHighest() {
            if (grades == null || grades.length == 0) return 0;
            int max = grades[0];
            for (int g : grades) if (g > max) max = g;
            return max;
        }

        int getLowest() {
            if (grades == null || grades.length == 0) return 0;
            int min = grades[0];
            for (int g : grades) if (g < min) min = g;
            return min;
        }

        String gradesAsString() {
            if (grades == null || grades.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < grades.length; i++) {
                sb.append(grades[i]);
                if (i < grades.length - 1) sb.append(", ");
            }
            return sb.toString();
        }
    }

    // Data storage
    private final java.util.List<Student> students = new ArrayList<>(); // ArrayList usage

    // Swing components
    private final JTextField nameField = new JTextField();
    private final JTextField gradesField = new JTextField(); // comma-separated grades
    private final JTextField searchField = new JTextField();
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> sortCombo;
    private final JButton sortButton;
    private final JButton addBtn, updateBtn, deleteBtn, searchBtn, exportPdfBtn, chartBtn;

    public StudentGradeTrackerAdvanced() {
        setTitle("Student Grade Tracker - Advanced (Swing)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Top panel - data entry
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Add / Update Student"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6,6,6,6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; topPanel.add(new JLabel("Student Name:"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0; topPanel.add(nameField, c);

        c.gridx = 0; c.gridy = 1; c.weightx = 0; topPanel.add(new JLabel("Grades (comma separated):"), c);
        c.gridx = 1; c.gridy = 1; c.weightx = 1.0; topPanel.add(gradesField, c);

        addBtn = new JButton("Add Student");
        updateBtn = new JButton("Update Selected");
        deleteBtn = new JButton("Delete Selected");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; topPanel.add(buttonPanel, c);

        add(topPanel, BorderLayout.NORTH);

        // Center - table
        String[] cols = {"Name", "Grades", "Average", "Highest", "Lowest"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 2) return Double.class;
                return String.class;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Students"));

        add(tableScroll, BorderLayout.CENTER);

        // Right panel - controls, search, sort, report/chart/export
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        // Search section
        JPanel searchPanel = new JPanel(new BorderLayout(6,6));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchBtn = new JButton("Search");
        searchPanel.add(searchBtn, BorderLayout.EAST);
        rightPanel.add(searchPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0,10)));

        // Sort section
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        sortPanel.setBorder(BorderFactory.createTitledBorder("Sort"));
        sortCombo = new JComboBox<>(new String[] {"Name (A-Z)", "Average (High→Low)", "Average (Low→High)", "Highest (High→Low)", "Lowest (Low→High)"});
        sortPanel.add(sortCombo);
        sortButton = new JButton("Sort");
        sortPanel.add(sortButton);
        rightPanel.add(sortPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0,10)));

        // Export / chart section
        JPanel reportPanel = new JPanel(new GridLayout(0,1,6,6));
        reportPanel.setBorder(BorderFactory.createTitledBorder("Reports & Charts"));
        exportPdfBtn = new JButton("Export Report to PDF");
        chartBtn = new JButton("Show Bar Chart (Averages)");
        reportPanel.add(chartBtn);
        reportPanel.add(exportPdfBtn);
        rightPanel.add(reportPanel);

        add(rightPanel, BorderLayout.EAST);

        // Wire up button actions
        addBtn.addActionListener(e -> onAddStudent());
        updateBtn.addActionListener(e -> onUpdateStudent());
        deleteBtn.addActionListener(e -> onDeleteStudent());
        searchBtn.addActionListener(e -> onSearch());
        sortButton.addActionListener(e -> onSort());
        chartBtn.addActionListener(e -> onShowChart());
        exportPdfBtn.addActionListener(e -> onExportPdf());

        // Double click on table to load selected into input fields
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int r = table.getSelectedRow();
                    if (r >= 0) loadSelectedToFields(r);
                }
            }
        });

        // Seed with example data
        seedExampleData();
        refreshTable();
    }

    // parse grades from text like "85, 90, 78"
    private int[] parseGrades(String text) throws NumberFormatException {
        String[] parts = text.split(",");
        ArrayList<Integer> list = new ArrayList<>();
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            int g = Integer.parseInt(t);
            if (g < 0) g = 0;
            list.add(g);
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private void onAddStudent() {
        String name = nameField.getText().trim();
        String gradesText = gradesField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter student name.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int[] g = parseGrades(gradesText);
            students.add(new Student(name, g));
            refreshTable();
            clearInputs();
            JOptionPane.showMessageDialog(this, "Student added.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid grades. Use comma separated integers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onUpdateStudent() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a student row to update.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String name = nameField.getText().trim();
        String gradesText = gradesField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter student name.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int[] newGrades = parseGrades(gradesText);
            // find student by selected row's name (names may not be unique in a real system - for demo we use index)
            Student s = students.get(sel);
            s.name = name;
            s.grades = newGrades;
            refreshTable();
            clearInputs();
            JOptionPane.showMessageDialog(this, "Student updated.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid grades. Use comma separated integers.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDeleteStudent() {
        int sel = table.getSelectedRow();
        if (sel < 0) {
            JOptionPane.showMessageDialog(this, "Select a student row to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete selected student?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            students.remove(sel);
            refreshTable();
            clearInputs();
        }
    }

    private void onSearch() {
        String term = searchField.getText().trim().toLowerCase();
        if (term.isEmpty()) {
            refreshTable();
            return;
        }
        // filter students by name contains or exact match
        DefaultTableModel tm = (DefaultTableModel) table.getModel();
        tm.setRowCount(0);
        for (Student s : students) {
            if (s.name.toLowerCase().contains(term)) {
                tm.addRow(new Object[]{ s.name, s.gradesAsString(), round(s.getAverage()), s.getHighest(), s.getLowest() });
            }
        }
    }

    private void onSort() {
        String sel = (String) sortCombo.getSelectedItem();
        if (sel == null) return;

        Comparator<Student> comp = Comparator.comparing(st -> st.name.toLowerCase());
        switch (sel) {
            case "Name (A-Z)":
                comp = Comparator.comparing(st -> st.name.toLowerCase());
                break;
            case "Average (High→Low)":
                comp = (a,b) -> Double.compare(b.getAverage(), a.getAverage());
                break;
            case "Average (Low→High)":
                comp = Comparator.comparingDouble(Student::getAverage);
                break;
            case "Highest (High→Low)":
                comp = (a,b) -> Integer.compare(b.getHighest(), a.getHighest());
                break;
            case "Lowest (Low→High)":
                comp = Comparator.comparingInt(Student::getLowest);
                break;
        }
        Collections.sort(students, comp);
        refreshTable();
    }

    private void onShowChart() {
        JFreeChart chart = createChartForAverages();
        ChartPanel panel = new ChartPanel(chart);
        JFrame f = new JFrame("Student Averages Chart");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(panel);
        f.setSize(800, 500);
        f.setLocationRelativeTo(this);
        f.setVisible(true);
    }

    private void onExportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save PDF Report");
        chooser.setSelectedFile(new File("StudentReport.pdf"));
        int res = chooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;
        File f = chooser.getSelectedFile();
        try {
            exportReportToPdf(f);
            JOptionPane.showMessageDialog(this, "PDF exported to: " + f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to export PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Create a bar chart plotting each student's average
    private JFreeChart createChartForAverages() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Student s : students) {
            dataset.addValue(s.getAverage(), "Average", s.name);
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Student Averages",
                "Student",
                "Average",
                dataset,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        return chart;
    }

    // Export textual report + embedded chart image to PDF using PDFBox
    private void exportReportToPdf(File outFile) throws Exception {
        PDDocument doc = new PDDocument();
        try {
            // first page with textual report
            PDPage page = new PDPage(PDRectangle.LETTER);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
            cs.newLineAtOffset(40, 720);
            cs.showText("Student Grade Report");
            cs.endText();

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, 10);
            cs.newLineAtOffset(40, 700);

            // Write report lines, wrap if necessary
            for (Student s : students) {
                String line = String.format("Name: %s | Grades: %s | Average: %.2f | Highest: %d | Lowest: %d",
                        s.name, s.gradesAsString(), s.getAverage(), s.getHighest(), s.getLowest());
                // if y position low, create new page
                cs.showText(line);
                cs.newLineAtOffset(0, -14);
            }
            cs.endText();
            cs.close();

            // create chart image
            JFreeChart chart = createChartForAverages();
            int w = 700, h = 350;
            BufferedImage chartImage = chart.createBufferedImage(w, h);

            // create a new page for chart
            PDPage chartPage = new PDPage(PDRectangle.LETTER);
            doc.addPage(chartPage);

            // convert bufferedimage to byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(chartImage, "png", baos);
            baos.flush();
            byte[] imgBytes = baos.toByteArray();
            baos.close();

            PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, imgBytes, "chart");
            PDPageContentStream imgStream = new PDPageContentStream(doc, chartPage);
            // center image
            float scale = 0.9f;
            float imgWidth = pdImage.getWidth() * scale;
            float imgHeight = pdImage.getHeight() * scale;
            float startX = (chartPage.getMediaBox().getWidth() - imgWidth) / 2;
            float startY = (chartPage.getMediaBox().getHeight() - imgHeight) / 2;
            imgStream.drawImage(pdImage, startX, startY, imgWidth, imgHeight);
            imgStream.close();

            // save
            doc.save(outFile);
        } finally {
            doc.close();
        }
    }

    // Refresh table from students list
    private void refreshTable() {
        DefaultTableModel tm = tableModel;
        tm.setRowCount(0);
        for (Student s : students) {
            tm.addRow(new Object[]{ s.name, s.gradesAsString(), round(s.getAverage()), s.getHighest(), s.getLowest() });
        }
    }

    private void loadSelectedToFields(int row) {
        if (row < 0 || row >= students.size()) return;
        Student s = students.get(row);
        nameField.setText(s.name);
        gradesField.setText(s.gradesAsString());
    }

    private void clearInputs() {
        nameField.setText("");
        gradesField.setText("");
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void seedExampleData() {
        students.add(new Student("Alice", new int[]{85, 92, 78}));
        students.add(new Student("Bob", new int[]{70, 66, 77}));
        students.add(new Student("Charlie", new int[]{95, 90, 93}));
        students.add(new Student("Dana", new int[]{58, 64, 70}));
    }

    public static void main(String[] args) {
        // Run on EDT
        SwingUtilities.invokeLater(() -> {
            StudentGradeTrackerAdvanced app = new StudentGradeTrackerAdvanced();
            app.setVisible(true);
        });
    }
}
