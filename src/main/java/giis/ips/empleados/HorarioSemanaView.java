package giis.ips.empleados;

import com.toedter.calendar.JDateChooser;
import lombok.Getter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.Color;

@Getter
public class HorarioSemanaView {

    private JFrame frame;

    private JTable tabEmpleados;
    private JDateChooser dcSemana;
    private JButton btnAnterior;
    private JButton btnSiguiente;
    private JLabel lblSemana;
    private HorarioSemanalPanel panelHorario;
    private JLabel lblTotal;
    private JButton btnCerrar;

    public HorarioSemanaView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame();
        frame.setTitle("Mi horario semanal");
        frame.setName("HorarioSemana");
        frame.setBounds(0, 0, 1000, 900);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); // No se cierra la app al cerrar
        frame.getContentPane().setLayout(new MigLayout("", "[right][grow]", "[][100:150][][][grow]"));

        frame.getContentPane().add(new JLabel("Selecciona tu nombre:"), "span 2, left, wrap");
        tabEmpleados = new JTable();
        tabEmpleados.setName("tabEmpleados");
        tabEmpleados.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabEmpleados.setDefaultEditor(Object.class, null); // Tabla de solo lectura
        frame.getContentPane().add(new JScrollPane(tabEmpleados), "span 2, grow, wrap");

        // Seleccion de la semana: cualquier dia de ella o las flechas para moverse de semana en semana
        JLabel lbl = new JLabel("Semana del día:");
        dcSemana = new JDateChooser();
        dcSemana.setDateFormatString("yyyy-MM-dd");
        dcSemana.setName("dcSemana");
        lbl.setLabelFor(dcSemana);
        frame.getContentPane().add(lbl);
        btnAnterior = new JButton("< Semana anterior");
        btnAnterior.setName("btnAnterior");
        btnSiguiente = new JButton("Semana siguiente >");
        btnSiguiente.setName("btnSiguiente");
        frame.getContentPane().add(dcSemana, "split 3, w 150!");
        frame.getContentPane().add(btnAnterior);
        frame.getContentPane().add(btnSiguiente, "wrap");

        lblSemana = new JLabel(" ");
        lblSemana.setName("lblSemana");
        frame.getContentPane().add(lblSemana, "span 2, left, wrap");
        panelHorario = new HorarioSemanalPanel();
        JScrollPane spHorario = new JScrollPane(panelHorario);
        spHorario.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        frame.getContentPane().add(spHorario, "span 2, grow, wrap");
        lblTotal = new JLabel(" ");
        lblTotal.setName("lblTotal");
        frame.getContentPane().add(lblTotal, "span 2, left, wrap");

        btnCerrar = new JButton("Cerrar");
        btnCerrar.setName("btnCerrar");
        frame.getContentPane().add(btnCerrar, "span 2, right, gaptop 15");
    }
}
