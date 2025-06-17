#!/bin/bash

# LapXpert Configuration Validation Script
# This script validates that all required environment variables are set

set -e

echo "🔍 Validating LapXpert Configuration..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Validation results
ERRORS=0
WARNINGS=0

# Function to check required environment variable
check_required() {
    local var_name=$1
    local description=$2
    
    if [ -z "${!var_name}" ]; then
        echo -e "${RED}❌ REQUIRED: $var_name is not set${NC}"
        echo "   Description: $description"
        ((ERRORS++))
    else
        echo -e "${GREEN}✅ $var_name is set${NC}"
    fi
}

# Function to check optional environment variable
check_optional() {
    local var_name=$1
    local description=$2
    local default_value=$3
    
    if [ -z "${!var_name}" ]; then
        echo -e "${YELLOW}⚠️  OPTIONAL: $var_name is not set (will use default: $default_value)${NC}"
        echo "   Description: $description"
        ((WARNINGS++))
    else
        echo -e "${GREEN}✅ $var_name is set${NC}"
    fi
}

echo ""
echo "📊 Database Configuration"
check_optional "DATABASE_URL" "PostgreSQL connection string" "jdbc:postgresql://lapxpert-db.khoalda.dev:5432/lapxpert8?user=lapxpert&password=lapxpert!"
check_optional "DB_POOL_SIZE" "Database connection pool size" "3"

echo ""
echo "📧 Email Configuration"
check_optional "MAIL_HOST" "SMTP server host" "smtp.gmail.com"
check_optional "MAIL_PORT" "SMTP server port" "587"
check_optional "MAIL_USERNAME" "Email username" "empty"
check_optional "MAIL_PASSWORD" "Email password" "empty"

echo ""
echo "🗄️ MinIO Configuration"
check_optional "MINIO_URL" "MinIO server URL" "https://lapxpert-storage-api.khoalda.dev"
check_optional "MINIO_ACCESS_KEY" "MinIO access key" "empty"
check_optional "MINIO_SECRET_KEY" "MinIO secret key" "empty"

echo ""
echo "🔴 Redis Configuration"
check_optional "REDIS_HOST" "Redis server host" "lapxpert-redis.khoalda.dev"
check_optional "REDIS_PORT" "Redis server port" "6379"

echo ""
echo "💳 VNPay Configuration"
check_optional "VNPAY_TMN_CODE" "VNPay terminal code" "4FWARVVC"
check_optional "VNPAY_HASH_SECRET" "VNPay hash secret" "7UG6NK3YS9C59FYCM1F7UHOT8H2INKAP"

echo ""
echo "🔐 Security Configuration"
check_optional "JWT_SECRET" "JWT signing secret" "lapxpert_secret_key_for_development_only"
check_optional "JWT_EXPIRATION_HOURS" "JWT token expiration hours" "5"

echo ""
echo "👤 Admin Configuration"
check_optional "DEFAULT_ADMIN_ENABLED" "Enable default admin user" "true"
check_optional "DEFAULT_ADMIN_EMAIL" "Default admin email" "admin@lapxpert.com"
check_optional "DEFAULT_ADMIN_PASSWORD" "Default admin password" "admin123456"

echo ""
echo "🌐 Frontend Configuration"
check_optional "VITE_API_BASE_URL" "Frontend API base URL" "http://localhost:8080/api"

echo ""
echo "📋 Validation Summary"
echo "=================="

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ Configuration validation passed!${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠️  $WARNINGS optional variables are using default values${NC}"
        echo "   Consider setting them for production environments"
    fi
    exit 0
else
    echo -e "${RED}❌ Configuration validation failed!${NC}"
    echo -e "${RED}   $ERRORS required variables are missing${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}   $WARNINGS optional variables are using default values${NC}"
    fi
    echo ""
    echo "Please set the missing environment variables and run this script again."
    echo "See docs/environment-setup.md for detailed configuration instructions."
    exit 1
fi
