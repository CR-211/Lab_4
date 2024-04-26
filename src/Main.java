import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Philosopher extends Thread {
    private int name;
    private PhilosopherState currentState;
    private Forks forks;
    private static int currentName = 0;
    private Main.GraphPanel graphPanel;
    private volatile boolean paused = false;
    private Lock eatLock;
    private static final String LOG_FILE_NAME = "philosopher_log.txt";
    private FileWriter writer;

    public Philosopher(Forks forks, Main.GraphPanel graphPanel) {
        this.forks = forks;
        this.currentState = PhilosopherState.Hungry;
        this.name = currentName++;
        System.out.println("Philosopher #" + name + " has reached the table!");
        this.graphPanel = graphPanel;
        this.eatLock = new ReentrantLock();
        try {
            new FileWriter(LOG_FILE_NAME).close();
            writer = new FileWriter(LOG_FILE_NAME, true); // Open the file for appending
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logAction(String action) {
        try {
            writer.write("Philosopher #" + name + ": " + action + "\n");
            writer.flush(); // Make sure data is written to file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (true) {
            if (!isPaused()) {
                switch (currentState) {
                    case Hungry:
                        System.out.println("Philosopher #" + name + " is hungry");
                        logAction("is hungry");
                        graphPanel.updatePhilosopherState(name, PhilosopherState.Hungry);
                        try {
                            eatLock.lock();
                            while (!forks.takeForks(name)) {
                                Thread.sleep(100);
                            }
                            this.currentState = PhilosopherState.Eating;
                            graphPanel.updatePhilosopherState(name, PhilosopherState.Eating);
                            forks.decreaseFoodQuantity();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            eatLock.unlock();
                        }
                        break;
                    case Eating:
                        System.out.println("Philosopher #" + name + " is eating");
                        logAction("is eating");
                        try {
                            Thread.sleep((int) (Math.random() * 4000));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        forks.leaveForks(name);
                        this.currentState = PhilosopherState.Thinking;
                        graphPanel.updatePhilosopherState(name, PhilosopherState.Thinking);
                        break;
                    case Thinking:
                        System.out.println("Philosopher #" + name + " is thinking");
                        logAction("is thinking");
                        try {
                            Thread.sleep((int) (Math.random() * 10000));
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        this.currentState = PhilosopherState.Hungry;
                        break;
                }
            } else {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public PhilosopherState getCurrentState() {
        return currentState;
    }

    public void pauseThread() {
        paused = true;
    }

    public void resumeThread() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }
}

class Forks {
    private int number;
    private boolean[] forks;
    private Lock lock = new ReentrantLock();
    private int foodQuantity = 43;

    public Forks(int number) {
        this.number = number;
        this.forks = new boolean[number];
        for (int i = 0; i < number; i++) {
            forks[i] = true;
        }
        System.out.println("Forks are created!");
    }

    public boolean takeForks(int philosopherId) {
        lock.lock();
        try {
            int leftForkIndex = philosopherId;
            int rightForkIndex = (philosopherId + 1) % number;

            if (forks[leftForkIndex] && forks[rightForkIndex]) {
                forks[leftForkIndex] = false;
                forks[rightForkIndex] = false;
                System.out.println("Philosopher #" + philosopherId + " has taken the forks.");
                return true;
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public void leaveForks(int philosopherId) {
        lock.lock();
        try {
            int leftForkIndex = philosopherId;
            int rightForkIndex = (philosopherId + 1) % number;

            forks[leftForkIndex] = true;
            forks[rightForkIndex] = true;
            System.out.println("Philosopher #" + philosopherId + " has left the forks.");
        } finally {
            lock.unlock();
        }
    }

    public synchronized void decreaseFoodQuantity() {
        foodQuantity--;
        System.out.println("Food quantity: " + foodQuantity);
        if (foodQuantity == 0) {
            System.out.println("Food is finished. Stopping the simulation.");
            System.exit(0);
        }
    }

    public synchronized int getFoodQuantity() {
        return foodQuantity;
    }
}

enum PhilosopherState {Hungry(Color.RED), Eating(Color.BLUE), Thinking(Color.YELLOW);

    private final Color color;

    PhilosopherState(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

public class Main {
    static class GraphPanel extends JPanel {
        private PhilosopherState[] philosopherStates;
        private Forks forks;
        private int number;

        public GraphPanel(int number, Forks forks) {
            this.forks = forks;
            this.number = number;
            philosopherStates = new PhilosopherState[number];
            for (int i = 0; i < number; i++) {
                philosopherStates[i] = PhilosopherState.Hungry;
            }
        }

        public void updatePhilosopherState(int philosopherId, PhilosopherState newState) {
            philosopherStates[philosopherId] = newState;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int radius = Math.min(getWidth(), getHeight()) / 3;
            int circleRadius = Math.min(getWidth(), getHeight()) / 6;
            double angleIncrement = 2 * Math.PI / philosopherStates.length;

            int foodQuantity = forks.getFoodQuantity();
            g.setColor(Color.GREEN);
            g.fillOval(centerX - circleRadius / 2, centerY - circleRadius / 2, circleRadius, circleRadius);
            g.setColor(Color.BLACK);
            g.drawString(String.valueOf(foodQuantity), centerX - 5, centerY + 5);

            for (int i = 0; i < philosopherStates.length; i++) {
                int x = (int) (centerX + radius * Math.cos(i * angleIncrement));
                int y = (int) (centerY + radius * Math.sin(i * angleIncrement));
                g.setColor(philosopherStates[i].getColor());
                g.fillOval(x - circleRadius / 2, y - circleRadius / 2, circleRadius, circleRadius);
                g.setColor(Color.BLACK);
                g.drawString("P" + i + ": " + philosopherStates[i], x - 29, y + 5);

                double forkAngle = i * angleIncrement + Math.toRadians(16);
                int xForkStart = (int) (centerX + (radius - circleRadius / 2) * Math.cos(forkAngle));
                int yForkStart = (int) (centerY + (radius - circleRadius / 2) * Math.sin(forkAngle));
                int xForkEnd = (int) (centerX + (radius + circleRadius / 2) * Math.cos(forkAngle));
                int yForkEnd = (int) (centerY + (radius + circleRadius / 2) * Math.sin(forkAngle));
                g.drawLine(xForkStart, yForkStart, xForkEnd, yForkEnd);
            }
        }
    }

    public static void main(String[] args) {
        int number = 11;
        Forks forks = new Forks(number);
        Philosopher[] philosophers = new Philosopher[number];
        GraphPanel graphPanel = new GraphPanel(number, forks);

        for (int i = 0; i < number; i++) {
            philosophers[i] = new Philosopher(forks, graphPanel);
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Dining Philosophers");
            frame.setSize(800, 600);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel circlePanel = graphPanel;
            frame.add(circlePanel, BorderLayout.CENTER);

            JButton pauseResumeButton = new JButton("Pause/Resume");
            pauseResumeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    for (Philosopher philosopher : philosophers) {
                        if (!philosopher.isPaused()) {
                            philosopher.pauseThread();
                            pauseResumeButton.setText("Resume");
                        } else {
                            philosopher.resumeThread();
                            pauseResumeButton.setText("Pause");
                        }
                    }
                }
            });
            frame.add(pauseResumeButton, BorderLayout.SOUTH);

            frame.setVisible(true);

            for (Philosopher philosopher : philosophers) {
                philosopher.start();
            }
        });
    }
}
