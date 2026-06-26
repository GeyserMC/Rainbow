/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

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
