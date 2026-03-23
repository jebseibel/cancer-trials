# Seed Data Reference

This document describes the seed data that will be loaded into the database on first run.

## Overview

The seed data includes **20 food items** with complete nutritional information, flavor profiles, and serving sizes, perfect for building custom salads.

## Food Categories

### 🥬 Leafy Greens (4 items)
1. **Romaine Lettuce** (`ROMAINE`) - Crisp and mild base
2. **Fresh Spinach** (`SPINACH`) - Tender nutrient-dense greens
3. **Curly Kale** (`KALE`) - Hearty superfood
4. **Fresh Arugula** (`ARUGULA`) - Peppery and bold

### 🥕 Vegetables (5 items)
5. **Cherry Tomatoes** (`TOMATO`) - Sweet and juicy
6. **English Cucumber** (`CUCUMBR`) - Cool and refreshing
7. **Shredded Carrots** (`CARROT`) - Sweet and crunchy
8. **Bell Pepper Mix** (`BELLPEP`) - Colorful and sweet
9. **Red Onion** (`REDONION`) - Sharp and tangy

### 🍗 Proteins (4 items)
10. **Grilled Chicken Breast** (`CHICKEN`) - Lean poultry protein
11. **Hard Boiled Egg** (`HARDEGG`) - Classic protein
12. **Roasted Chickpeas** (`CHICKPEA`) - Plant-based protein
13. **Feta Cheese** (`FETA`) - Tangy and creamy

### 🥜 Nuts & Seeds (3 items)
14. **Sliced Almonds** (`ALMOND`) - Crunchy and toasted
15. **Chopped Walnuts** (`WALNUT`) - Rich and earthy
16. **Sunflower Seeds** (`SUNFLWR`) - Nutty and crunchy

### 🥗 Dressings (4 items)
17. **Balsamic Vinaigrette** (`BALSAMIC`) - Light and tangy
18. **Ranch Dressing** (`RANCH`) - Classic creamy
19. **Caesar Dressing** (`CAESAR`) - Garlicky and rich
20. **Extra Virgin Olive Oil** (`OLIVEOIL`) - Simple and healthy

## Nutritional Data

All nutrition values are per 100g serving and include:
- **Carbohydrates** (g)
- **Fat** (g)
- **Protein** (g)
- **Sugar** (g)

Example (Romaine Lettuce):
- Carbs: 3g | Fat: 0g | Protein: 1g | Sugar: 1g

## Flavor Profiles

Each food has a flavor profile rated 1-5 for:
- **Crunch** - Texture and crispness
- **Punch** - Boldness and intensity
- **Sweet** - Sweetness level
- **Savory** - Umami and savory notes

Example (Arugula):
- Crunch: 2 | Punch: 4 | Sweet: 1 | Savory: 2

## Serving Sizes

Standard serving conversions include:
- **Cup** (g)
- **Quarter Cup** (g)
- **Tablespoon** (g)
- **Teaspoon** (g)
- **Gram** (standard 100g)

## Sample Salad Ideas

### Mediterranean Salad
- Romaine Lettuce (2 cups)
- Cherry Tomatoes (1 cup)
- Cucumber (1 cup)
- Red Onion (1/4 cup)
- Feta Cheese (1/4 cup)
- Olive Oil (2 tbsp)

### Protein Power Salad
- Spinach (2 cups)
- Grilled Chicken (1 cup)
- Hard Boiled Egg (1)
- Chickpeas (1/2 cup)
- Bell Peppers (1/2 cup)
- Balsamic Vinaigrette (2 tbsp)

### Superfood Crunch Salad
- Kale (2 cups)
- Carrots (1/2 cup)
- Almonds (1/4 cup)
- Sunflower Seeds (2 tbsp)
- Cranberries
- Caesar Dressing (2 tbsp)

## Database Tables Populated

1. **nutrition** - 20 nutritional profiles
2. **flavor** - 20 flavor profiles
3. **serving** - 20 serving size profiles
4. **food** - 20 food items (references nutrition, flavor, serving)
5. **profile** - 5 user profiles (already existed)
6. **customer** - 5 customers (already existed)

## Using the Data

### Via REST API
```bash
# Get all foods
GET http://localhost:8080/api/food

# Build a salad
POST http://localhost:8080/api/salad/build
{
  "ingredients": [
    {"foodExtid": "food-romaine-001", "quantity": 2},
    {"foodExtid": "food-chicken-001", "quantity": 1},
    {"foodExtid": "food-tomato-001", "quantity": 0.5}
  ]
}
```

### Via Frontend
1. Navigate to **Foods** page to see all ingredients
2. Go to **Salad Builder** to create custom salads
3. Add ingredients and see real-time nutrition totals
4. View aggregated flavor profiles

## Data File Locations

All seed data is in Liquibase format:
- `src/main/resources/db/changelog/changes/sql/003_load_nutrition.sql`
- `src/main/resources/db/changelog/changes/sql/004_load_flavor.sql`
- `src/main/resources/db/changelog/changes/sql/005_load_serving.sql`
- `src/main/resources/db/changelog/changes/sql/006_load_food.sql`

These files are automatically executed by Liquibase on application startup.

---

**Happy Salad Building!** 🥗
