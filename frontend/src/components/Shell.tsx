import { Link, NavLink, Outlet } from "react-router-dom";
import type { JSX } from "react";
import { clearBearer } from "../lib/auth";
import SearchBar from "./SearchBar";

/**
 * 대시보드 쉘 — 헤더 + 좌 사이드 + 본문.
 *
 * 좌측 영역은 `Sidebar`(폴더 트리). 본문은 라우터 Outlet.
 *
 * @since phase 11
 */
export default function Shell(): JSX.Element {
  return (
    <div className="flex h-screen flex-col bg-ink-900 text-ink-100">
      <header className="flex h-12 items-center justify-between gap-4 border-b border-ink-700 px-4">
        <Link to="/" className="font-semibold tracking-tight">
          Mneme
        </Link>
        <SearchBar />
        <nav className="flex items-center gap-1 text-sm text-ink-300">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            메모리
          </NavLink>
          <NavLink
            to="/archive"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            아카이브
          </NavLink>
          <NavLink
            to="/map"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            맵
          </NavLink>
          <NavLink
            to="/lint"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            검토
          </NavLink>
          <NavLink
            to="/keys"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            키
          </NavLink>
          <NavLink
            to="/connect"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            연결
          </NavLink>
          <NavLink
            to="/data"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            데이터
          </NavLink>
          <NavLink
            to="/usage"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            사용량
          </NavLink>
          <NavLink
            to="/audit"
            className={({ isActive }) =>
              `rounded px-2 py-1 hover:text-ink-100 ${isActive ? "text-ink-100" : ""}`
            }
          >
            감사
          </NavLink>
          <button
            type="button"
            onClick={() => {
              clearBearer();
              window.location.reload();
            }}
            className="ml-2 rounded border border-ink-600 px-2 py-1 text-xs hover:bg-ink-700"
          >
            로그아웃
          </button>
        </nav>
      </header>
      <div className="flex flex-1 min-h-0">
        <Outlet />
      </div>
    </div>
  );
}
