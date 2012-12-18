/* Copyright 2012 Simon Ley alias "skarute"
 * 
 * This file is part of Faunis.
 * 
 * Faunis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Faunis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General
 * Public License along with Faunis. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package communication.clientToButlerOrders;

/** The client tells the butler that he wants to log in. */
public class CBLoginOrder extends CBOrder {
	private static final long serialVersionUID = 1L;
	private String name;
	private String password;
	
	
	
	public CBLoginOrder(String name, String password){
		this.name = name;
		this.password = password;
	}
	
	
	
	public String getName(){
		return this.name;
	}
	
	
	
	public String getPassword(){
		return this.password;
	}
}
