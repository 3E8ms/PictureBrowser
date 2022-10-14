import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


class WrapLayout extends FlowLayout {
    private Dimension preferredLayoutSize;

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;

            while (container.getSize().width == 0 && container.getParent() != null) {
                container = container.getParent();
            }

            targetWidth = container.getSize().width;

            if (targetWidth == 0)
                targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            //  Fit components into the allowed width

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

            if (scrollPane != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }
}


class FImage {
    int star;
    File file;
    String path;
    JLabel icon;
    String date;

    FImage(File f) {
        this.star = 0;
        this.file = f;
        this.path = f.getAbsolutePath();
        this.icon = null;
        try {
            Path path = f.toPath();
            BasicFileAttributes fatr = Files.readAttributes(path, BasicFileAttributes.class);
            String time = fatr.creationTime().toString();
            this.date = time.substring(0, 10);
        } catch (Exception e) {
            System.out.println("Cannot find file in " + path);
        }
        try {
            BufferedImage newP = ImageIO.read(file);
            JLabel picLabel = new JLabel(new ImageIcon(newP.getScaledInstance(300, 200, Image.SCALE_SMOOTH)));
            this.icon = picLabel;
        } catch (Exception e) {
            System.out.println("File " + f.getName() + " is not a valid picture format.");
        }
    }

}

public class Main {
    public static void main(String[] args) {
        new FotagFrame("Fotag");
    }
}


class FotagFrame extends JFrame {

    private ArrayList<FImage> imagefile = new ArrayList<>();
    int view = 0;
    int rate = 0;

    public FotagFrame(String title) {
        super(title);
        this.setSize(1100, 768);
        setLocationRelativeTo(null);

        this.setMinimumSize(new Dimension(500, 400));
        Dimension ss = new Dimension(150, 30);
        this.setVisible(true);

        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (!imagefile.isEmpty()) {
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter("filelog.txt"));
                        writer.write(Integer.toString(view));
                        writer.newLine();
                        writer.write(Integer.toString(rate));
                        writer.newLine();
                        for (FImage f : imagefile) {
                            String str = "";
                            str += f.path;
                            str += " ";
                            str += f.date;
                            str += " ";
                            str += f.star;
                            writer.write(str);
                            writer.newLine();
                        }
                        writer.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        File file = new File("filelog.txt");
        if (file.exists() && file.length() > 0) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String st = "";
                st = br.readLine();
                view = Integer.valueOf(st);
                st = br.readLine();
                rate = Integer.valueOf(st);
                while ((st = br.readLine()) != null) {
                    String[] info = st.split(" ");
                    Path p = Paths.get(info[0]);
                    FImage newF = new FImage(p.toFile());
                    imagefile.add(newF);//path date star
                    newF.date = info[1];
                    newF.star = Integer.valueOf(info[2]);
                }
                br.close();
            } catch (Exception ex) {
                System.out.println("File reading error");
                file.deleteOnExit();
            }
        }


        JMenuBar mb = new JMenuBar();
        mb.setAlignmentY(Component.TOP_ALIGNMENT);
        mb.setBackground(Color.WHITE);
        mb.setLayout(new BoxLayout(mb, 0));
        this.setJMenuBar(mb);

        JButton ListV = new JButton("List View");
        JButton GridV = new JButton("Grid View");

        ListV.setBackground(Color.WHITE);
        GridV.setBackground(Color.WHITE);

        JLabel t = new JLabel("Fotag!");

        mb.add(ListV);
        mb.add(GridV);
        mb.add(Box.createHorizontalGlue());
        mb.add(t);
        mb.add(Box.createHorizontalGlue());

        JButton ImportB = new JButton("Import");
        JButton clear = new JButton("Clear");
        JButton delete = new JButton("Clear Contents");
        ImportB.setBackground(Color.WHITE);
        clear.setBackground(Color.WHITE);
        delete.setBackground(Color.white);
        mb.add(ImportB);

        // star filter
        JButton fj1 = new JButton();
        JButton fj2 = new JButton();
        JButton fj3 = new JButton();
        JButton fj4 = new JButton();
        JButton fj5 = new JButton();
        fj1.setBackground(Color.white);
        fj2.setBackground(Color.white);
        fj3.setBackground(Color.white);
        fj4.setBackground(Color.white);
        fj5.setBackground(Color.white);
        fj1.setSize(20, 20);
        fj2.setSize(20, 20);
        fj3.setSize(20, 20);
        fj4.setSize(20, 20);
        fj5.setSize(20, 20);
        try {
            BufferedImage emptystar = ImageIO.read(new File("icon/emptystar.png"));
            BufferedImage fullstar = ImageIO.read(new File("icon/fullstar.png"));
            fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            fj1.setBorder(null);
            fj2.setBorder(null);
            fj3.setBorder(null);
            fj4.setBorder(null);
            fj5.setBorder(null);
            if (rate >= 1) {
                fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                if (rate >= 2) {
                    fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    if (rate >= 3) {
                        fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 4) {
                            fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 5) {
                                fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            } else {
                                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                } else {
                    fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }
            } else {
                fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
            }

            fj1.setBorder(null);
            fj2.setBorder(null);
            fj3.setBorder(null);
            fj4.setBorder(null);
            fj5.setBorder(null);
            fj1.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rate = 1;
                    JScrollPane r = remake(imagefile, view, rate);
                    setContentPane(r);
                    revalidate();
                    repaint();
                }
            });
            fj1.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (rate >= 1) {
                        fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 2) {
                            fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 3) {
                                fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                if (rate >= 4) {
                                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    if (rate >= 5) {
                                        fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    } else {
                                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                }
            });

            fj2.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rate = 2;
                    JScrollPane r = remake(imagefile, view, rate);
                    setContentPane(r);
                    revalidate();
                    repaint();
                }
            });
            fj2.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (rate >= 1) {
                        fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 2) {
                            fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 3) {
                                fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                if (rate >= 4) {
                                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    if (rate >= 5) {
                                        fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    } else {
                                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                }
            });

            fj3.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rate = 3;
                    JScrollPane r = remake(imagefile, view, rate);
                    setContentPane(r);
                    revalidate();
                    repaint();
                }
            });
            fj3.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (rate >= 1) {
                        fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 2) {
                            fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 3) {
                                fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                if (rate >= 4) {
                                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    if (rate >= 5) {
                                        fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    } else {
                                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                }
            });

            fj4.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rate = 4;
                    JScrollPane r = remake(imagefile, view, rate);
                    setContentPane(r);
                    revalidate();
                    repaint();
                }
            });
            fj4.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (rate >= 1) {
                        fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 2) {
                            fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 3) {
                                fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                if (rate >= 4) {
                                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    if (rate >= 5) {
                                        fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    } else {
                                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                }
            });

            fj5.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    rate = 5;
                    JScrollPane r = remake(imagefile, view, rate);
                    setContentPane(r);
                    revalidate();
                    repaint();
                }
            });
            fj5.addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {

                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (rate >= 1) {
                        fj1.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        if (rate >= 2) {
                            fj2.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            if (rate >= 3) {
                                fj3.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                if (rate >= 4) {
                                    fj4.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    if (rate >= 5) {
                                        fj5.setIcon(new ImageIcon(fullstar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    } else {
                                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                                fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            }
                        } else {
                            fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                            fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        }
                    } else {
                        fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                        fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    }
                }
            });

        } catch (Exception e) {
            System.out.println("Star avatar is not found.");
        }
        JLabel j = new JLabel();
        j.setLayout(new FlowLayout(FlowLayout.CENTER));
        j.setSize(ss);
        j.setPreferredSize(ss);
        j.setMaximumSize(ss);
        j.setMinimumSize(ss);
        j.add(fj1);
        j.add(fj2);
        j.add(fj3);
        j.add(fj4);
        j.add(fj5);


        mb.add(j);
        mb.add(clear);
        mb.add(delete);

        JScrollPane scroller = remake(imagefile, view, rate);
        setContentPane(scroller);

        try {
            BufferedImage ListIcon = ImageIO.read(new File("icon/ListView.png"));
            ListV.setIcon(new ImageIcon(ListIcon));
            BufferedImage GridIcon = ImageIO.read(new File("icon/GridView.png"));
            GridV.setIcon(new ImageIcon(GridIcon));
            BufferedImage dIcon = ImageIO.read(new File("icon/delete.png"));
            delete.setIcon(new ImageIcon(dIcon));
            delete.setSize(new Dimension(45, 150));
            delete.setPreferredSize(new Dimension(150, 30));
            ListV.setBackground(Color.WHITE);
            GridV.setBackground(Color.WHITE);
            delete.setBackground(Color.white);
            ListV.setBorder(null);
            GridV.setBorder(null);
            delete.setBorder(null);
        } catch (Exception e) {

        }

        try {
            BufferedImage ImportIcon = ImageIO.read(new File("icon/ImportIcon.png"));
            ImportB.setIcon(new ImageIcon(ImportIcon));
            ImportB.setBorder(null);
            ImportB.setBackground(Color.WHITE);
        } catch (Exception e) {

        }

        GridV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                view = 0;
                ListV.setBackground(Color.white);
                GridV.setBackground(Color.lightGray);
                JScrollPane r = remake(imagefile, view, rate);
                setContentPane(r);
                revalidate();
                repaint();
            }
        });

        ListV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                view = 1;
                ListV.setBackground(Color.lightGray);
                GridV.setBackground(Color.white);
                JScrollPane r = remake(imagefile, view, rate);
                setContentPane(r);
                revalidate();
                repaint();
            }
        });


        ImportB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String cwd = System.getProperty("user.dir");
                JFileChooser fileChooser = new JFileChooser(cwd);
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setDialogTitle("Import File to Fotag!");
                int returnValue = fileChooser.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    File[] selectedFile = fileChooser.getSelectedFiles();
                    for (File f : selectedFile) {
                        boolean dup = false;
                        int size = imagefile.size();
                        for (int i = 0; i < size; i++) {
                            if (imagefile.get(i).path.equals(f.getAbsolutePath())) {
                                dup = true;
                            }
                        }
                        if (!dup) {
                            FImage newf = new FImage(f);
                            imagefile.add(newf);
                        }
                    }
                }
                JScrollPane r = remake(imagefile, view, rate);
                setContentPane(r);
                revalidate();
                repaint();
            }
        });

        clear.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rate = 0;
                try {
                    BufferedImage emptystar = ImageIO.read(new File("icon/emptystar.png"));
                    fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                } catch (Exception ex) {
                    System.out.println("Cannot find star avatar");
                }
                JScrollPane r = remake(imagefile, view, rate);
                setContentPane(r);
                revalidate();
                repaint();
            }
        });

        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rate = 0;
                view = 0;
                imagefile.clear();
                File file = new File("filelog.txt");
                try {
                    java.nio.file.Files.delete(file.toPath());
                } catch (Exception ex) {
                    System.out.println(ex);
                }
                try {
                    BufferedImage emptystar = ImageIO.read(new File("icon/emptystar.png"));
                    fj1.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj2.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj3.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj4.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                    fj5.setIcon(new ImageIcon(emptystar.getScaledInstance(20, 20, Image.SCALE_SMOOTH)));
                } catch (Exception ex) {
                    System.out.println("Cannot find star avatar");
                }

                JScrollPane r = remake(imagefile, view, rate);
                setContentPane(r);
                revalidate();
                repaint();

            }
        });
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    String printStar(int star) {
        if (star == 0) {
            return String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606");   //u2606 white star, u2605 black star
        }
        if (star == 1) {
            return String.valueOf("\u2605") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606");
        }
        if (star == 2) {
            return String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606");
        }
        if (star == 3) {
            return String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2606") + String.valueOf("\u2606");
        }
        if (star == 4) {
            return String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2606");
        }
        if (star == 5) {
            return String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605") + String.valueOf("\u2605");
        }
        return String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606") + String.valueOf("\u2606");
    }

    JScrollPane remake(ArrayList<FImage> imagefile, int view, int rate) {
        if (imagefile.isEmpty()) {
            try {
                BufferedImage empty = ImageIO.read(new File("icon/empty.png"));
                JLabel label = new JLabel(new ImageIcon(empty));
                JPanel np = new JPanel();
                JScrollPane scroller = new JScrollPane(np, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                np.setLayout(new BorderLayout());
                np.add(label);
                return scroller;
            } catch (Exception e) {
                System.out.println("Cannot find empty.png");
            }
        }
        Dimension boxsize = new Dimension(300, 270);
        if (view == 0) {        //grid view
            boxsize = new Dimension(300, 270);
        } else if (view == 1) {    //list view
            boxsize = new Dimension(4000, 210);
        }

        JPanel np = new JPanel();
        JScrollPane scroller = new JScrollPane(np, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        np.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));
        if (view == 0) {      //grid view
            np.setLayout(new WrapLayout(FlowLayout.LEFT, 10, 10));
        } else if (view == 1) {
            np.setLayout(new BoxLayout(np, BoxLayout.Y_AXIS));
        }
        for (FImage f : imagefile) {
            if (f.star >= rate || f.star == 0) {
                JButton b = new JButton();
                JPanel p = new JPanel();
                JPanel t = new JPanel();
                t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
                p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                if (view == 0) {
                    t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
                    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
                } else if (view == 1) {
                    t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
                    p.setLayout(new FlowLayout(FlowLayout.LEFT));
                }


                p.setPreferredSize(boxsize);
                p.setMaximumSize(boxsize);
                p.setMinimumSize(boxsize);

                p.add(f.icon);
                JLabel fn = new JLabel("File name: " + f.file.getName());
                JLabel cd = new JLabel("Create date: " + f.date);
                JLabel star = new JLabel("Rating: " + printStar(f.star));
                t.add(fn);
                t.add(cd);
                t.add(star);
                p.add(t);
                b.add(p);
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        Dimension r = new Dimension(800, 600);
                        Dimension s = new Dimension(300, 100);
                        JFrame frame = new JFrame("Rate " + f.file.getName());
                        frame.pack();
                        frame.setSize(800, 600);
                        frame.setMaximumSize(r);
                        frame.setMinimumSize(r);
                        frame.setPreferredSize(r);

                        JPanel panel = new JPanel(new BorderLayout());
                        frame.add(panel);
                        frame.setContentPane(panel);
                        frame.addWindowListener(new WindowListener() {
                            @Override
                            public void windowOpened(WindowEvent e) {

                            }

                            @Override
                            public void windowClosing(WindowEvent e) {
                                JScrollPane r = remake(imagefile, view, rate);
                                setContentPane(r);
                                revalidate();
                                repaint();
                            }

                            @Override
                            public void windowClosed(WindowEvent e) {

                            }

                            @Override
                            public void windowIconified(WindowEvent e) {

                            }

                            @Override
                            public void windowDeiconified(WindowEvent e) {

                            }

                            @Override
                            public void windowActivated(WindowEvent e) {

                            }

                            @Override
                            public void windowDeactivated(WindowEvent e) {

                            }
                        });

                        //rate panel
                        JButton j1 = new JButton();
                        JButton j2 = new JButton();
                        JButton j3 = new JButton();
                        JButton j4 = new JButton();
                        JButton j5 = new JButton();
                        j1.setBackground(Color.white);
                        j2.setBackground(Color.white);
                        j3.setBackground(Color.white);
                        j4.setBackground(Color.white);
                        j5.setBackground(Color.white);
                        j1.setSize(30, 30);
                        j2.setSize(30, 30);
                        j3.setSize(30, 30);
                        j4.setSize(30, 30);
                        j5.setSize(30, 30);
                        try {
                            BufferedImage emptystar = ImageIO.read(new File("icon/emptystar.png"));
                            BufferedImage fullstar = ImageIO.read(new File("icon/fullstar.png"));
                            j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            int n = f.star;
                            if (n >= 1) {
                                j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                if (n >= 2) {
                                    j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    if (n >= 3) {
                                        j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (n >= 4) {
                                            j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (n >= 5) {
                                                j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            } else {
                                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                } else {
                                    j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }
                            } else {
                                j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                            }

                            j1.setBorder(null);
                            j2.setBorder(null);
                            j3.setBorder(null);
                            j4.setBorder(null);
                            j5.setBorder(null);
                            j1.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    f.star = 1;
                                    star.setText("Rating: " + printStar(f.star));
                                }
                            });
                            j1.addMouseListener(new MouseListener() {
                                @Override
                                public void mouseClicked(MouseEvent e) {

                                }

                                @Override
                                public void mousePressed(MouseEvent e) {

                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {

                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    int num = f.star;
                                    if (num >= 1) {
                                        j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (num >= 2) {
                                            j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (num >= 3) {
                                                j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                if (num >= 4) {
                                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    if (num >= 5) {
                                                        j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    } else {
                                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    }
                                                } else {
                                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                }
                                            } else {
                                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                }
                            });

                            j2.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    f.star = 2;
                                    star.setText("Rating: " + printStar(f.star));
                                }
                            });
                            j2.addMouseListener(new MouseListener() {
                                @Override
                                public void mouseClicked(MouseEvent e) {

                                }

                                @Override
                                public void mousePressed(MouseEvent e) {

                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {

                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    int num = f.star;
                                    if (num >= 1) {
                                        j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (num >= 2) {
                                            j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (num >= 3) {
                                                j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                if (num >= 4) {
                                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    if (num >= 5) {
                                                        j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    } else {
                                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    }
                                                } else {
                                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                }
                                            } else {
                                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                }
                            });

                            j3.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    f.star = 3;
                                    star.setText("Rating: " + printStar(f.star));
                                }
                            });
                            j3.addMouseListener(new MouseListener() {
                                @Override
                                public void mouseClicked(MouseEvent e) {

                                }

                                @Override
                                public void mousePressed(MouseEvent e) {

                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {

                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    int num = f.star;
                                    if (num >= 1) {
                                        j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (num >= 2) {
                                            j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (num >= 3) {
                                                j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                if (num >= 4) {
                                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    if (num >= 5) {
                                                        j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    } else {
                                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    }
                                                } else {
                                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                }
                                            } else {
                                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                }
                            });

                            j4.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    f.star = 4;
                                    star.setText("Rating: " + printStar(f.star));
                                }
                            });
                            j4.addMouseListener(new MouseListener() {
                                @Override
                                public void mouseClicked(MouseEvent e) {

                                }

                                @Override
                                public void mousePressed(MouseEvent e) {

                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {

                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    int num = f.star;
                                    if (num >= 1) {
                                        j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (num >= 2) {
                                            j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (num >= 3) {
                                                j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                if (num >= 4) {
                                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    if (num >= 5) {
                                                        j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    } else {
                                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    }
                                                } else {
                                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                }
                                            } else {
                                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                }
                            });

                            j5.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    f.star = 5;
                                    star.setText("Rating: " + printStar(f.star));
                                }
                            });
                            j5.addMouseListener(new MouseListener() {
                                @Override
                                public void mouseClicked(MouseEvent e) {

                                }

                                @Override
                                public void mousePressed(MouseEvent e) {

                                }

                                @Override
                                public void mouseReleased(MouseEvent e) {

                                }

                                @Override
                                public void mouseEntered(MouseEvent e) {
                                    j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                }

                                @Override
                                public void mouseExited(MouseEvent e) {
                                    int num = f.star;
                                    if (num >= 1) {
                                        j1.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        if (num >= 2) {
                                            j2.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            if (num >= 3) {
                                                j3.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                if (num >= 4) {
                                                    j4.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    if (num >= 5) {
                                                        j5.setIcon(new ImageIcon(fullstar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    } else {
                                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    }
                                                } else {
                                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                }
                                            } else {
                                                j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                                j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            }
                                        } else {
                                            j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                            j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        }
                                    } else {
                                        j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                        j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    }
                                }
                            });
                        } catch (Exception c) {
                            System.out.println("Star avatar is not found.");
                        }
                        JLabel j = new JLabel();
                        j.setLayout(new FlowLayout(FlowLayout.CENTER));
                        try {
                            BufferedImage newP = ImageIO.read(f.file);
                            JLabel pl = new JLabel(new ImageIcon(newP));
                            panel.add(pl, BorderLayout.CENTER);
                        } catch (Exception ex) {

                        }
                        JButton clear = new JButton("Clear");
                        clear.setBackground(Color.white);
                        clear.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                f.star = 0;
                                star.setText("Rating: " + printStar(f.star));
                                try {
                                    BufferedImage emptystar = ImageIO.read(new File("icon/emptystar.png"));
                                    j1.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j2.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j3.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j4.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                    j5.setIcon(new ImageIcon(emptystar.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                                } catch (Exception ex) {
                                    System.out.println("Cannot find star avatar");
                                }
                            }
                        });
                        j.setSize(s);
                        j.setPreferredSize(s);
                        j.setMaximumSize(s);
                        j.setMinimumSize(s);
                        j.add(j1);
                        j.add(j2);
                        j.add(j3);
                        j.add(j4);
                        j.add(j5);
                        j.add(clear);

                        panel.add(j, BorderLayout.SOUTH);
                        frame.setVisible(true);
                        revalidate();
                        repaint();
                    }
                });
                b.setBackground(Color.white);
                np.add(b);
            }
        }
        return scroller;
    }
}