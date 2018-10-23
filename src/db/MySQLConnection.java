package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import entity.Item;
import entity.Item.ItemBuilder;
import external.YelpAPI;

public class MySQLConnection {

	private Connection connection;
	
	public MySQLConnection(){
		try{
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			connection = DriverManager.getConnection(MySQLDBUtil.URL);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void close() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setFavoriteItems(String userId, List<String> itemIds) {
		if(connection == null){
			System.out.println("DB connection failed!");
			return;
		}
		try{
			String sql = "INSERT IGNORE INTO history (user_id, item_id) VALUES (?, ?)";
			PreparedStatement pStatement = connection.prepareStatement(sql);
			pStatement.setString(1, userId);
			for(String itemId : itemIds){
				pStatement.setString(2, itemId);
				pStatement.execute();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if(connection == null){
			System.out.println("DB connection failed!");
			return;
		}
		try{
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			PreparedStatement pStatement = connection.prepareStatement(sql);
			pStatement.setString(1, userId);
			for(String itemId : itemIds){
				pStatement.setString(2, itemId);
				pStatement.execute();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public Set<String> getFavoriteItemIds(String userId) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return new HashSet<>();
		}
		Set<String> favoriteItemIds = new HashSet<>();
		
		try {
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				favoriteItemIds.add(rs.getString("item_id"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		return favoriteItemIds;

	}

	public Set<Item> getFavoriteItems(String userId) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return new HashSet<>();
		}
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);

		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			
			for (String itemId : itemIds) {
				ps.setString(1, itemId);
				ResultSet rs = ps.executeQuery();
				
				ItemBuilder builder = new ItemBuilder();
				while (rs.next()) {
					builder.setItemId(rs.getString("item_id"));
					builder.setName(rs.getString("name"));
					builder.setAddress(rs.getString("address"));
					builder.setUrl(rs.getString("url"));
					builder.setImageUrl(rs.getString("image_url"));
					builder.setRating(rs.getDouble("rating"));
					builder.setDistance(rs.getDouble("distance"));
					builder.setCategories(getCategories(itemId));
					
					favoriteItems.add(builder.build());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		return favoriteItems;

	}

	public Set<String> getCategories(String itemId) {
		if (connection == null) {
			System.err.println("DB connection failed");
			return null;
		}
		Set<String> categories = new HashSet<>();
		
		try {
			String sql = "SELECT category FROM categories WHERE item_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, itemId);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				categories.add(rs.getString("category"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		return categories;

	}

	public List<Item> searchItems(double lat, double lon, String term) {
		YelpAPI api = new YelpAPI();
		
		List<Item> items = api.search(lat, lon, term);
		
		for(Item item : items){
			saveItem(item);
		}
		
		return items;
	}

	public void saveItem(Item item) {
		if(connection == null){
			System.err.println("DB connection failed");
			return;
		}
		try{
			//check if item exist
			String sqlcheck = "SELECT item_id FROM items WHERE item_id = ?";
			PreparedStatement pStatement = connection.prepareStatement(sqlcheck);
			pStatement.setString(1, item.getItemId());
			ResultSet rs = pStatement.executeQuery();
			
			if(rs.next()){
				//update item info
				String sqlUpdate = "UPDATE items SET name = ?, rating = ?, address = ?, url = ?, image_url = ?, distance = ? WHERE item_id = ?";
				pStatement = connection.prepareStatement(sqlUpdate);
				pStatement.setString(1, item.getName());
				pStatement.setDouble(2, item.getRating());
				pStatement.setString(3, item.getAddress());
				pStatement.setString(4, item.getUrl());
				pStatement.setString(5, item.getImageUrl());
				pStatement.setDouble(6, item.getDistance());
				pStatement.setString(7, item.getItemId());
				pStatement.execute();
			}else{
				//insert new
				String sqlInsert = "INSERT IGNORE INTO items VALUES (?, ?, ?, ?, ?, ?, ?)";
				pStatement = connection.prepareStatement(sqlInsert);
				pStatement.setString(1, item.getItemId());
				pStatement.setString(2, item.getName());
				pStatement.setDouble(3, item.getRating());
				pStatement.setString(4, item.getAddress());
				pStatement.setString(5, item.getUrl());
				pStatement.setString(6, item.getImageUrl());
				pStatement.setDouble(7, item.getDistance());
				pStatement.execute();
			}
			//update category table
			String sqlCat = "DELETE FROM categories WHERE item_id = ?";
			pStatement = connection.prepareStatement(sqlCat);
			pStatement.setString(1, item.getItemId());
			
			sqlCat = "INSERT IGNORE INTO categories VALUES (?, ?)";
			pStatement = connection.prepareStatement(sqlCat);
			pStatement.setString(1, item.getItemId());
			for(String category : item.getCategories()){
				pStatement.setString(2,  category);
				pStatement.execute();
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//login verify
	public boolean verifyLogin(String userId, String password){
			
		if (connection == null) {
			System.err.println("DB connection failed");
			return false;
		}
		
		try {
			String sql = "SELECT password FROM users WHERE user_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return password.equals(rs.getString("password"));
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	//get full name from user id
	public String getFullname(String userId){
		if (connection == null) {
			System.err.println("DB connection failed");
		}
		
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				String firstName = rs.getString("first_name");
				String lastName = rs.getString("last_name");
				return firstName + " " + lastName;
			} else {
				return "";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	//verify user id at registration
	public boolean verifyUserId(String userId){
		if (connection == null) {
			System.err.println("DB connection failed");
		}
		try{
			String sql = "SELECT * FROM users where user_id = ?";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				return false;
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return true;
	}
	
	//regster a new user
	public void registerNewUser(String userId, String pwd, String firstName, String lastName){
		
		if (connection == null) {
			System.err.println("DB connection failed");
		}
		try{
			String sql = "INSERT IGNORE INTO users VALUES (?, ?, ?, ?)";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, userId);
			ps.setString(2, pwd);
			ps.setString(3, firstName);
			ps.setString(4, lastName);
			ps.execute();
		}catch (Exception e){
			e.printStackTrace();
		}

	}
	
}
