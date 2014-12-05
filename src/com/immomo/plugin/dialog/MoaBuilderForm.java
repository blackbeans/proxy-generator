package com.immomo.plugin.dialog;

import javax.swing.*;
import java.awt.event.*;

public class MoaBuilderForm extends JDialog {
    private JPanel contentPanel;
    private JPanel jp_guide;
    private JButton jb_cancel;
    private JPanel jp_confirm;
    private JTextField jt_serviceUri;
    private JTextField jt_servicePort;
    private JTextField jt_svn;
    private JTextField jt_deployPath;
    private JTextField jt_interfaceName;
    private JButton jb_ok;


    private IBuilderFormListener listener ;

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public static interface IBuilderFormListener {
        void onOk(MoaBuilderArgs args);
    }

    public JTextField getJt_serviceUri() {
        return jt_serviceUri;
    }

    public JTextField getJt_deployPath() {
        return jt_deployPath;
    }

    public MoaBuilderForm(IBuilderFormListener listener) {

        this.listener = listener;

        setContentPane(contentPanel);
        setModal(true);
        getRootPane().setDefaultButton(jb_ok);

        jb_ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        jb_cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        jp_guide.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {

        String serviceUri =this.jt_serviceUri.getText();
        String servicePort = this.jt_servicePort.getText();
        String svnUrl = this.jt_svn.getText();
        String deployPath = this.jt_deployPath.getText();
        String interfaceName = this.jt_interfaceName.getText();

        MoaBuilderArgs args = new MoaBuilderArgs();
        //写入参数
        args.setServiceUri(serviceUri);
        args.setServicePort(Integer.parseInt(servicePort));
        args.setSvn(svnUrl);
        args.setDeployPath(deployPath);
        args.setInterfaceName(interfaceName);

        this.listener.onOk(args);

// add your code here
        dispose();
    }




    private void onCancel() {
// add your code here if necessary
        dispose();
    }
}
