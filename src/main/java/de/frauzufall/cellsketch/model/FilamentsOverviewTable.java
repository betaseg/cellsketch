/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package de.frauzufall.cellsketch.model;

public class FilamentsOverviewTable {

	public static String getCountColumnName() {
		return "number of filaments";
	}

	public static String getTotalLengthColumnName() {
		return "total length in micrometer";
	}

	public static String getMeanLengthColumnName() {
		return "mean length in micrometer";
	}

	public static String getStdevLengthColumnName() {
		return "stdev length in micrometer";
	}

	public static String getMedianLengthColumnName() {
		return "median length in micrometer";
	}

	public static String getMeanTortuosityColumnName() {
		return "mean tortuosity in micrometer";
	}

	public static String getStdevTortuosityColumnName() {
		return "stdev tortuosity in micrometer";
	}

	public static String getMedianTortuosityColumnName() {
		return "median tortuosity in micrometer";
	}

	public static String getPercentageConnectedToColumnName(String other) {
		return "percentage connected to " + other;
	}

	public static String getNumberConnectedToColumnName(String other) {
		return "number connected to " + other;
	}

	public static String getNumberDisconnectedFromColumn(String other) {
		return "number disconnected from " + other;
	}

}
