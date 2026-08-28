import java.util.Arrays;

/**
 * Trabalho: Simulacao e Metodos Analiticos - 2026/2
 * Modulo 4 | Desenvolvimento de Simulador para uma Fila
 *
 * Integrantes do grupo:
 *   - Arthur Ferreira
 *   - Julia Melo
 *   - Murilo Silva
 *   - Rafael Richter
 *
 * Simulador de fila por eventos discretos (DES) para filas G/G/c/K:
 *   - c servidores, capacidade total K (incluindo quem esta sendo atendido)
 *   - tempos entre chegadas e de atendimento sorteados uniformemente em [min, max]
 *
 * O gerador de numeros pseudoaleatorios segue o Metodo Congruente Linear pedido
 * no enunciado: Xn+1 = (a*Xn + c) mod M, normalizado para [0,1) em NextRandom().
 * Parametros atuais: a=16807, c=0, M=2^31-1.
 */
public class QueueSimulator {

    // Gerador Congruente Linear (Xn+1 = (a*Xn + c) mod M) 
    private final long a;
    private final long c;
    private final long m;
    private long xn; // ultimo numero gerado da sequencia (estado do gerador)

    private int randomsUsed = 0;

    public QueueSimulator(long seed) {
        this(seed, 16807L, 0L, 2147483647L);
    }

    public QueueSimulator(long seed, long a, long c, long m) {
        this.a = a;
        this.c = c;
        this.m = m;
        this.xn = seed; // X0 = semente inicial
    }

    //NextRandom(): gera o proximo numero da sequencia, normalizado entre 0 e 1. 
    private double nextUniform01() {
        xn = (a * xn + c) % m;
        randomsUsed++;
        return (double) xn / (double) m;
    }

    private double uniform(double min, double max) {
        return min + (max - min) * nextUniform01();
    }

    static class Resultado {
        int numServers;
        int capacity;
        double[] stateTime;   // tempo acumulado em cada estado 
        int perdas;
        double tempoGlobal;
    }

    public Resultado simular(int numServers, int capacity,
                              double arrMin, double arrMax,
                              double servMin, double servMax,
                              double firstArrival, int maxRandoms) {

        randomsUsed = 0;

        double clock = 0.0;
        int inSystem = 0; // clientes no sistema (em atendimento e os que estão esperando)
        int perdas = 0;

        double[] stateTime = new double[capacity + 1];

        // tempo em que cada servidor fica livre; 
        double[] serverFreeAt = new double[numServers];
        Arrays.fill(serverFreeAt, Double.POSITIVE_INFINITY);

        double nextArrival = firstArrival;

        while (randomsUsed < maxRandoms) {

            double nextDeparture = Double.POSITIVE_INFINITY;
            int serverIdx = -1;
            for (int i = 0; i < numServers; i++) {
                if (serverFreeAt[i] < nextDeparture) {
                    nextDeparture = serverFreeAt[i];
                    serverIdx = i;
                }
            }

            boolean isArrival = nextArrival <= nextDeparture;
            double eventTime = isArrival ? nextArrival : nextDeparture;

            // acumula o tempo passado no estado atual, antes de mudar de estado
            stateTime[inSystem] += (eventTime - clock);
            clock = eventTime;

            if (isArrival) {
                if (inSystem < capacity) {
                    inSystem++;
                    int freeServer = -1;
                    for (int i = 0; i < numServers; i++) {
                        if (serverFreeAt[i] == Double.POSITIVE_INFINITY) {
                            freeServer = i;
                            break;
                        }
                    }
                    if (freeServer != -1) {
                        double serviceTime = uniform(servMin, servMax);
                        serverFreeAt[freeServer] = clock + serviceTime;
                    }
                    // se nao achou servidor livre, cliente fica esperando na fila
                } else {
                    perdas++; // sistema cheio: cliente perdido
                }

                double interArrival = uniform(arrMin, arrMax);
                nextArrival = clock + interArrival;

                if (randomsUsed >= maxRandoms) break;

            } else {
                inSystem--;
                serverFreeAt[serverIdx] = Double.POSITIVE_INFINITY;

                // se ainda ha clientes esperando, inicia o proximo atendimento
                if (inSystem >= numServers) {
                    double serviceTime = uniform(servMin, servMax);
                    serverFreeAt[serverIdx] = clock + serviceTime;
                }

                if (randomsUsed >= maxRandoms) break;
            }
        }

        Resultado r = new Resultado();
        r.numServers = numServers;
        r.capacity = capacity;
        r.stateTime = stateTime;
        r.perdas = perdas;
        r.tempoGlobal = clock;
        return r;
    }

    /**
     * Modo de depuracao: roda a simulacao imprimindo cada evento processado
     * (tempo, tipo, estado resultante) para os primeiros 'traceLimit' eventos,
     * util para conferir a logica manualmente.
     */
    public void simularComTrace(int numServers, int capacity,
                                 double arrMin, double arrMax,
                                 double servMin, double servMax,
                                 double firstArrival, int traceLimit) {

        randomsUsed = 0;
        double clock = 0.0;
        int inSystem = 0;
        int perdas = 0;

        double[] serverFreeAt = new double[numServers];
        Arrays.fill(serverFreeAt, Double.POSITIVE_INFINITY);
        double nextArrival = firstArrival;

        System.out.println("--- TRACE (primeiros " + traceLimit + " eventos) ---");
        System.out.println("evento# | tempo    | tipo     | clientes_no_sistema | perdas");

        int evento = 0;
        while (evento < traceLimit) {
            double nextDeparture = Double.POSITIVE_INFINITY;
            int serverIdx = -1;
            for (int i = 0; i < numServers; i++) {
                if (serverFreeAt[i] < nextDeparture) {
                    nextDeparture = serverFreeAt[i];
                    serverIdx = i;
                }
            }

            boolean isArrival = nextArrival <= nextDeparture;
            double eventTime = isArrival ? nextArrival : nextDeparture;
            clock = eventTime;

            if (isArrival) {
                if (inSystem < capacity) {
                    inSystem++;
                    int freeServer = -1;
                    for (int i = 0; i < numServers; i++) {
                        if (serverFreeAt[i] == Double.POSITIVE_INFINITY) { freeServer = i; break; }
                    }
                    if (freeServer != -1) {
                        double serviceTime = uniform(servMin, servMax);
                        serverFreeAt[freeServer] = clock + serviceTime;
                    }
                } else {
                    perdas++;
                }
                double interArrival = uniform(arrMin, arrMax);
                nextArrival = clock + interArrival;
            } else {
                inSystem--;
                serverFreeAt[serverIdx] = Double.POSITIVE_INFINITY;
                if (inSystem >= numServers) {
                    double serviceTime = uniform(servMin, servMax);
                    serverFreeAt[serverIdx] = clock + serviceTime;
                }
            }

            evento++;
            System.out.printf("  %3d   | %8.4f | %-8s |         %d           |  %d%n",
                    evento, clock, (isArrival ? "chegada" : "saida"), inSystem, perdas);
        }
        System.out.println();
    }

    private static void imprimir(Resultado r) {
        System.out.printf("=== G/G/%d/%d ===%n", r.numServers, r.capacity);
        System.out.printf("Tempo global da simulacao: %.4f%n", r.tempoGlobal);
        System.out.println("Numero de clientes perdidos: " + r.perdas);
        System.out.println("Estado | Tempo acumulado | Probabilidade");
        for (int s = 0; s <= r.capacity; s++) {
            double prob = r.stateTime[s] / r.tempoGlobal;
            System.out.printf("  %d    | %14.4f | %.6f%n", s, r.stateTime[s], prob);
        }
        System.out.println();
    }

    public static void main(String[] args) {
        long seed = 42L; 

        //MODO TESTE:
        // Roda so os primeiros 15 eventos com trace, pode conferir a mao
        // (recalculando os NextRandom() 
        QueueSimulator simTrace = new QueueSimulator(seed);
        simTrace.simularComTrace(1, 5, 2, 5, 3, 5, 3.0, 15);

        //Parte 1 (entrega de feedback):
        // chegadas entre 3...5, atendimento entre 4...5, fila vazia, 1o cliente em t=3.0
        System.out.println(">>> ENTREGA FEEDBACK (chegadas 3...5, atendimento 4...5)");
        QueueSimulator sim1 = new QueueSimulator(seed);
        Resultado ggg1 = sim1.simular(1, 5, 3, 5, 4, 5, 3.0, 100_000);
        imprimir(ggg1);

        QueueSimulator sim2 = new QueueSimulator(seed);
        Resultado ggg2 = sim2.simular(2, 5, 3, 5, 4, 5, 3.0, 100_000);
        imprimir(ggg2);

        //Tabela final (chegadas 2...5, atendimento 3...5):
        System.out.println(">>> TABELA FINAL (chegadas 2...5, atendimento 3...5)");
        QueueSimulator sim3 = new QueueSimulator(seed);
        Resultado ggg1b = sim3.simular(1, 5, 2, 5, 3, 5, 3.0, 100_000);
        imprimir(ggg1b);

        QueueSimulator sim4 = new QueueSimulator(seed);
        Resultado ggg2b = sim4.simular(2, 5, 2, 5, 3, 5, 3.0, 100_000);
        imprimir(ggg2b);
    }
}
