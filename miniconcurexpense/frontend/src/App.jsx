import React, { useState, useEffect } from "react";
import axios from "axios";
import { Card, CardContent } from "./components/ui/card";
import { Button } from "./components/ui/button";

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || "";

export default function App() {
  const [tenant, setTenant] = useState("public");
  const [schemas, setSchemas] = useState(["public"]);
  const [expenses, setExpenses] = useState([]);
  const [description, setDescription] = useState("");
  const [amount, setAmount] = useState("");
  const [page, setPage] = useState(0);
  const pageSize = 20;
  const [totalPages, setTotalPages] = useState(1);
  const [selectedIds, setSelectedIds] = useState(new Set());

  const headersForTenant = (t) =>
    t && t !== "public"
      ? { "X-Tenant": t }
      : {};

  const fetchSchemas = async () => {
    try {
      const resp = await axios.get(`${BACKEND_URL}/debug/schemas`);
      const list = Array.isArray(resp.data) ? resp.data : [];
      // ensure 'public' at least
      if (!list.includes("public")) list.unshift("public");
      setSchemas(list);
      if (!list.includes(tenant)) {
        setTenant("public");
        setPage(0);
      }
    } catch (err) {
      console.error("Error fetching schemas", err);
    }
  };

  const fetchExpenses = async (requestedPage = page) => {
    try {
      const resp = await axios.get(
        `${BACKEND_URL}/expenses?page=${requestedPage}&size=${pageSize}`,
        { headers: headersForTenant(tenant) }
      );
      const data = resp.data;
      if (Array.isArray(data.content)) {
        setExpenses(data.content);
        setTotalPages(data.totalPages ?? 1);
        setPage(data.number ?? requestedPage);
      } else if (Array.isArray(data)) {
        setExpenses(data);
        setTotalPages(1);
        setPage(0);
      } else {
        setExpenses([]);
      }
      // clear selection when page changes / data reloads
      setSelectedIds(new Set());
    } catch (err) {
      console.error("Error fetching expenses", err);
    }
  };

  const createExpense = async () => {
    try {
      await axios.post(
        `${BACKEND_URL}/expenses`,
        { description, amount: parseFloat(amount) },
        { headers: headersForTenant(tenant) }
      );
      setDescription("");
      setAmount("");
      setPage(0);
      fetchExpenses(0);
    } catch (err) {
      console.error("Error creating expense", err);
    }
  };

  useEffect(() => {
    fetchSchemas();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    fetchExpenses(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenant, page]);

  const prevPage = () => {
    if (page > 0) setPage((p) => p - 1);
  };
  const nextPage = () => {
    if (page + 1 < totalPages) setPage((p) => p + 1);
  };

  // toggle row selection by id
  const toggleSelect = (id) => {
    setSelectedIds((prev) => {
      const copy = new Set(prev);
      if (copy.has(id)) copy.delete(id);
      else copy.add(id);
      return copy;
    });
  };

  const deleteSelected = async () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`Delete ${selectedIds.size} selected expense(s)?`)) return;
    try {
      // send deletes in parallel
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          axios.delete(`${BACKEND_URL}/expenses/${id}`, { headers: headersForTenant(tenant) })
        )
      );
      // refresh current page
      fetchExpenses(page);
    } catch (err) {
      console.error("Error deleting expenses", err);
      // still refresh to keep UI consistent
      fetchExpenses(page);
    }
  };

  // Utility to detect if a given id is selected
  const isSelected = (id) => selectedIds.has(id);

  return (
    <div className="p-4 max-w-3xl mx-auto">
      <h1 className="text-2xl font-bold mb-4">Mini Concur Expense</h1>

      <div className="grid grid-cols-3 gap-2 mb-4 items-center">
        <select
          className="col-span-2 border rounded p-2"
          value={tenant}
          onChange={(e) => {
            setTenant(e.target.value);
            setPage(0);
          }}
        >
          {schemas.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
        <Button onClick={() => fetchExpenses(0)}>Load</Button>
      </div>

      <Card className="mb-4">
        <CardContent>
          <h2 className="text-xl font-semibold mb-2">Add Expense</h2>
          <input
            className="w-full border rounded p-2 mb-2"
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <input
            className="w-full border rounded p-2 mb-2"
            placeholder="Amount"
            type="number"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />
          <Button onClick={createExpense}>Add</Button>
        </CardContent>
      </Card>

      <div className="flex justify-between items-center mb-2">
        <h2 className="text-xl font-semibold">Expenses (page {page + 1} of {totalPages})</h2>
        <div className="flex items-center gap-2">
          <Button onClick={deleteSelected} disabled={selectedIds.size === 0}>
            Delete selected ({selectedIds.size})
          </Button>
        </div>
      </div>

      <div className="mb-4 overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr>
              <th className="border-b py-2">ID</th>
              <th className="border-b py-2">Description</th>
              <th className="border-b py-2">Amount</th>
            </tr>
          </thead>
          <tbody>
            {expenses.length === 0 && (
              <tr>
                <td colSpan="3" className="py-4 text-center text-gray-400">No expenses</td>
              </tr>
            )}
            {expenses.map((exp) => {
              const selected = isSelected(exp.id);
              return (
                <tr
                  key={exp.id}
                  onClick={() => toggleSelect(exp.id)}
                  style={{
                    cursor: "pointer",
                    backgroundColor: selected ? "rgba(59,130,246,0.12)" : "transparent"
                  }}
                >
                  <td className="py-2">{exp.id}</td>
                  <td className="py-2">{exp.description}</td>
                  <td className="py-2">${Number(exp.amount).toFixed(2)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      <div className="flex justify-between items-center">
        <Button onClick={prevPage} disabled={page <= 0}>
          Previous
        </Button>
        <div>
          Page {page + 1} / {totalPages}
        </div>
        <Button onClick={nextPage} disabled={page + 1 >= totalPages}>
          Next
        </Button>
      </div>
    </div>
  );
}

