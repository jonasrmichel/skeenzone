/**
 * SkeenZone
 * http://code.google.com/p/skeenzone
 * 
 * Copyright 2011 Kyle Prete, Jonas Michel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utexas.skeenzone;

import java.io.Serializable;
import java.util.Random;

public class SessionIdentifier implements Serializable {

	private static final long serialVersionUID = -550507858953411287L;

	public long id;
	private static Random randy_ = new Random();

	private SessionIdentifier(long id) {
		this.id = id;
	}

	public static SessionIdentifier createID() {
		long id = randy_.nextLong();
		SessionIdentifier s = new SessionIdentifier(id);
		return s;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof SessionIdentifier))
			return false;
		SessionIdentifier s = (SessionIdentifier) other;
		return this.id == s.id;
	}

	@Override
	public int hashCode() {
		return (int) (this.id ^ (this.id >>> 32));
	}

	@Override
	public String toString() {
		return "" + id;
	}

}
