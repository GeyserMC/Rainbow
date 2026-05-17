package org.geysermc.rainbow;

import net.minecraft.util.ProblemReporter;

public final class ProblemSuccessReporter implements ProblemReporter {
    private final ProblemSuccessReporter origin;
    private final ProblemReporter delegate;
    private int problemsSeen = 0;

    private ProblemSuccessReporter(ProblemSuccessReporter origin, ProblemReporter delegate) {
        this.origin = origin;
        this.delegate = delegate;
    }

    public ProblemSuccessReporter(ProblemReporter delegate) {
        this.origin = this;
        this.delegate = delegate;
    }

    @Override
    public ProblemSuccessReporter forChild(PathElement path) {
        return new ProblemSuccessReporter(origin, delegate.forChild(path));
    }

    @Override
    public void report(Problem problem) {
        origin.problemsSeen++;
        delegate.report(problem);
    }

    public void reportSuccess(Problem notAProblem) {
        delegate.report(notAProblem);
    }

    public int problemsSeen() {
        return problemsSeen;
    }
}
