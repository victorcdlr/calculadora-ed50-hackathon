package com.edu.netbeans.ed50calculatorapp3;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.xy.*;

public class ED50CalculatorApp3 extends JFrame {
    private JTable doseTable;
    private JComboBox<String> sedanteComboBox;

    public ED50CalculatorApp3(String title) {
        super(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Tabla de entrada
        String[] columns = {"Dosis (mg/kg)", "N° de ratones", "Respondieron", "Latencia (min)", "Duración (min)"};
        DefaultTableModel model = new DefaultTableModel(columns, 5);
        doseTable = new JTable(model);

        // Selector de sedante
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Resultados de la ED50 y gráficos"));
        String[] sedantes = {"Pentobarbital", "Etanol"};
        sedanteComboBox = new JComboBox<>(sedantes);
        topPanel.add(new JLabel("Sedante:"));
        topPanel.add(sedanteComboBox);

        // Botón de cálculo
        JButton calcButton = new JButton("Calcular ED50");
        calcButton.addActionListener(e -> calcularED50());

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(calcButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(doseTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void calcularED50() {
        DefaultTableModel model = (DefaultTableModel) doseTable.getModel();
        int rowCount = model.getRowCount();
        String sedanteSeleccionado = sedanteComboBox.getSelectedItem().toString();

        double[] doses = new double[rowCount];
        double[] responses = new double[rowCount];

        try {
            for (int i = 0; i < rowCount; i++) {
                Object doseVal = model.getValueAt(i, 0);
                Object nTotal = model.getValueAt(i, 1);
                Object nResp = model.getValueAt(i, 2);
                if (doseVal == null || nTotal == null || nResp == null) continue;

                double dose = Double.parseDouble(doseVal.toString());
                int total = Integer.parseInt(nTotal.toString());
                int responded = Integer.parseInt(nResp.toString());

                if (total == 0) continue;

                doses[i] = dose;
                responses[i] = (double) responded / total;
            }

            // Ajustar dosis según vía y sedante
            double[] oralDoses = ajustarDosisSegunSedante(doses.clone(), "Oral", sedanteSeleccionado);
            double[] ipDoses = ajustarDosisSegunSedante(doses.clone(), "Intraperitoneal", sedanteSeleccionado);
            double[] ivDoses = ajustarDosisSegunSedante(doses.clone(), "Intravenosa", sedanteSeleccionado);
            double[] scDoses = ajustarDosisSegunSedante(doses.clone(), "Subcutánea", sedanteSeleccionado);

            double ed50Oral = estimateED50Simple(oralDoses, responses);
            double ed50IP = estimateED50Simple(ipDoses, responses);
            double ed50IV = estimateED50Simple(ivDoses, responses);
            double ed50SC = estimateED50Simple(scDoses, responses);

            mostrarVentanaResultados(ed50Oral, ed50IP, ed50IV, ed50SC, sedanteSeleccionado);
            mostrarGraficas(oralDoses, responses, ed50Oral, "Oral",
                            ipDoses, responses, ed50IP, "Intraperitoneal",
                            ivDoses, responses, ed50IV, "Intravenosa",
                            scDoses, responses, ed50SC, "Subcutánea", sedanteSeleccionado);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error en los datos: " + ex.getMessage());
        }
    }

    private double estimateED50Simple(double[] doses, double[] responses) {
        for (int i = 1; i < doses.length; i++) {
            if (responses[i - 1] < 0.5 && responses[i] >= 0.5) {
                double slope = (responses[i] - responses[i - 1]) / (doses[i] - doses[i - 1]);
                return doses[i - 1] + (0.5 - responses[i - 1]) / slope;
            }
        }
        return -1;
    }

    private void mostrarVentanaResultados(double ed50Oral, double ed50IP, double ed50IV, double ed50SC, String sedante) {
        JFrame resultFrame = new JFrame("Resultados de la ED50");
        resultFrame.setLayout(new BorderLayout());

        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayout(5, 1));
        resultPanel.add(new JLabel("ED50 estimada para " + sedante + " según la vía de administración:"));
        resultPanel.add(new JLabel(String.format("Oral: %.2f mg/kg", ed50Oral)));
        resultPanel.add(new JLabel(String.format("Intraperitoneal: %.2f mg/kg", ed50IP)));
        resultPanel.add(new JLabel(String.format("Intravenosa: %.2f mg/kg", ed50IV)));
        resultPanel.add(new JLabel(String.format("Subcutánea: %.2f mg/kg", ed50SC)));

        resultFrame.add(resultPanel, BorderLayout.CENTER);
        resultFrame.setSize(300, 200);
        resultFrame.setLocationRelativeTo(null);
        resultFrame.setVisible(true);
    }

    private void mostrarGraficas(double[] oralDoses, double[] responsesOral, double ed50Oral, String routeOral,
                                 double[] ipDoses, double[] responsesIP, double ed50IP, String routeIP,
                                 double[] ivDoses, double[] responsesIV, double ed50IV, String routeIV,
                                 double[] scDoses, double[] responsesSC, double ed50SC, String routeSC,
                                 String sedante) {

        XYSeries puntosOral = new XYSeries("Datos experimentales - " + routeOral);
        XYSeries puntosIP = new XYSeries("Datos experimentales - " + routeIP);
        XYSeries puntosIV = new XYSeries("Datos experimentales - " + routeIV);
        XYSeries puntosSC = new XYSeries("Datos experimentales - " + routeSC);

        for (int i = 0; i < oralDoses.length; i++) {
            puntosOral.add(oralDoses[i], responsesOral[i]);
            puntosIP.add(ipDoses[i], responsesIP[i]);
            puntosIV.add(ivDoses[i], responsesIV[i]);
            puntosSC.add(scDoses[i], responsesSC[i]);
        }

        XYSeries curvaOral = new XYSeries("Curva logística simulada - " + routeOral);
        XYSeries curvaIP = new XYSeries("Curva logística simulada - " + routeIP);
        XYSeries curvaIV = new XYSeries("Curva logística simulada - " + routeIV);
        XYSeries curvaSC = new XYSeries("Curva logística simulada - " + routeSC);

        double maxDoseOral = Arrays.stream(oralDoses).max().orElse(1);
        for (double x = 0; x <= maxDoseOral * 1.2; x += 0.1) {
            curvaOral.add(x, 1.0 / (1.0 + Math.exp(-4 * (x - ed50Oral) / ed50Oral)));
            curvaIP.add(x, 1.0 / (1.0 + Math.exp(-4 * (x - ed50IP) / ed50IP)));
            curvaIV.add(x, 1.0 / (1.0 + Math.exp(-4 * (x - ed50IV) / ed50IV)));
            curvaSC.add(x, 1.0 / (1.0 + Math.exp(-4 * (x - ed50SC) / ed50SC)));
        }

        XYSeriesCollection datasetOral = new XYSeriesCollection();
        datasetOral.addSeries(puntosOral);
        datasetOral.addSeries(curvaOral);

        XYSeriesCollection datasetIP = new XYSeriesCollection();
        datasetIP.addSeries(puntosIP);
        datasetIP.addSeries(curvaIP);

        XYSeriesCollection datasetIV = new XYSeriesCollection();
        datasetIV.addSeries(puntosIV);
        datasetIV.addSeries(curvaIV);

        XYSeriesCollection datasetSC = new XYSeriesCollection();
        datasetSC.addSeries(puntosSC);
        datasetSC.addSeries(curvaSC);

        JFreeChart chartOral = ChartFactory.createXYLineChart("Curva Dosis-Respuesta (" + sedante + "): " + routeOral,
                "Dosis (mg/kg)", "Proporción que respondió", datasetOral, PlotOrientation.VERTICAL, true, true, false);
        JFreeChart chartIP = ChartFactory.createXYLineChart("Curva Dosis-Respuesta (" + sedante + "): " + routeIP,
                "Dosis (mg/kg)", "Proporción que respondió", datasetIP, PlotOrientation.VERTICAL, true, true, false);
        JFreeChart chartIV = ChartFactory.createXYLineChart("Curva Dosis-Respuesta (" + sedante + "): " + routeIV,
                "Dosis (mg/kg)", "Proporción que respondió", datasetIV, PlotOrientation.VERTICAL, true, true, false);
        JFreeChart chartSC = ChartFactory.createXYLineChart("Curva Dosis-Respuesta (" + sedante + "): " + routeSC,
                "Dosis (mg/kg)", "Proporción que respondió", datasetSC, PlotOrientation.VERTICAL, true, true, false);

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new ChartPanel(chartOral));
        panel.add(new ChartPanel(chartIP));
        panel.add(new ChartPanel(chartIV));
        panel.add(new ChartPanel(chartSC));

        JFrame frame = new JFrame("Gráficas Dosis-Respuesta");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Este método ahora tiene en cuenta la vía y el sedante para ajustar dosis más realistas
    private double[] ajustarDosisSegunSedante(double[] dosis, String route, String sedante) {
        double multiplicador = 1.0;

        if (sedante.equals("Pentobarbital")) {
            switch (route) {
                case "Intraperitoneal" -> multiplicador = 1.5;
                case "Intravenosa" -> multiplicador = 0.4;
                case "Subcutánea" -> multiplicador = 1.2;
                default -> multiplicador = 1.0;
            }
        } else if (sedante.equals("Etanol")) {
            switch (route) {
                case "Intraperitoneal" -> multiplicador = 1.8; // Mayor variabilidad y metabolismo hepático
                case "Intravenosa" -> multiplicador = 0.6;
                case "Subcutánea" -> multiplicador = 1.5;
                default -> multiplicador = 1.0;
            }
        }

        for (int i = 0; i < dosis.length; i++) {
            dosis[i] = dosis[i] * multiplicador;
        }
        return dosis;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ED50CalculatorApp3("Calculadora de ED50"));
    }
}